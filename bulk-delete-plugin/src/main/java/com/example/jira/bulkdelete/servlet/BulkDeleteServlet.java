package com.example.jira.bulkdelete.servlet;

import com.atlassian.jira.bc.issue.IssueService;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.project.ProjectManager;
import com.atlassian.jira.user.ApplicationUser;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collection;

public class BulkDeleteServlet extends HttpServlet {

    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {

        String projectKey = req.getParameter("projectKey");
        ProjectManager projectManager = ComponentAccessor.getProjectManager();
        Collection<Project> projects = projectManager.getProjects();

        StringBuilder projectOptions = new StringBuilder();
        for (Project project : projects) {
            boolean selected = project.getKey().equals(projectKey);
            projectOptions.append("<option value='")
                .append(project.getKey()).append("'")
                .append(selected ? " selected" : "").append(">")
                .append(project.getName())
                .append(" (").append(project.getKey()).append(")</option>");
        }

        StringBuilder issueCheckboxes = new StringBuilder();
        if (projectKey != null && !projectKey.isEmpty()) {
            Project project = projectManager.getProjectObjByKey(projectKey);
            if (project != null) {
                IssueManager issueManager = ComponentAccessor.getIssueManager();
                try {
                    Collection<Long> issueIds = issueManager.getIssueIdsForProject(project.getId());
                    if (issueIds.isEmpty()) {
                        issueCheckboxes.append("<p style='color:gray'>No issues found in this project.</p>");
                    } else {
                        issueCheckboxes.append("<table border='1' cellpadding='8' style='border-collapse:collapse;width:100%'>");
                        issueCheckboxes.append("<tr style='background:#f0f0f0'><th>Select</th><th>Key</th><th>Summary</th><th>Status</th></tr>");
                        for (Long issueId : issueIds) {
                            Issue issue = issueManager.getIssueObject(issueId);
                            if (issue != null) {
                                issueCheckboxes.append("<tr>")
                                    .append("<td style='text-align:center'><input type='checkbox' name='issueId' value='")
                                    .append(issue.getId()).append("'></td>")
                                    .append("<td>").append(issue.getKey()).append("</td>")
                                    .append("<td>").append(issue.getSummary()).append("</td>")
                                    .append("<td>").append(issue.getStatus().getName()).append("</td>")
                                    .append("</tr>");
                            }
                        }
                        issueCheckboxes.append("</table><br/>");
                        issueCheckboxes.append("<button type='submit' name='action' value='deleteSelected' style='padding:8px 16px;background:red;color:white;border:none;cursor:pointer;margin-right:10px'>Delete Selected</button>");
                        issueCheckboxes.append("<button type='submit' name='action' value='deleteAll' style='padding:8px 16px;background:darkred;color:white;border:none;cursor:pointer'>Delete All Issues</button>");
                    }
                } catch (Exception e) {
                    issueCheckboxes.append("<p style='color:red'>Error loading issues: ").append(e.getMessage()).append("</p>");
                }
            }
        }

        resp.setContentType("text/html");
        resp.getWriter().write(
            "<html><body style='font-family:Arial;padding:20px'>" +
            "<h2>Bulk Delete Project Issues</h2>" +
            "<form method='POST'>" +
            "<input type='hidden' name='projectKey' value='" + (projectKey != null ? projectKey : "") + "'>" +

            "<label><b>Select Project:</b></label><br/><br/>" +
            "<select name='projectKeySelect' style='padding:5px;width:300px'>" +
            projectOptions.toString() +
            "</select>" +
            "<button type='submit' name='action' value='loadIssues' style='padding:6px 12px;margin-left:10px;cursor:pointer'>Load Issues</button>" +
            "<br/><br/>" +

            issueCheckboxes.toString() +

            "</form>" +
            "</body></html>"
        );
    }

    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {

        String action = req.getParameter("action");
        String projectKey = req.getParameter("projectKeySelect");
        if (projectKey == null) projectKey = req.getParameter("projectKey");

        resp.setContentType("text/html");

        if ("loadIssues".equals(action)) {
            resp.sendRedirect("/plugins/servlet/bulk-delete-page?projectKey=" + projectKey);
            return;
        }

        try {
            ApplicationUser user = ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser();
            IssueManager issueManager = ComponentAccessor.getIssueManager();
            IssueService issueService = ComponentAccessor.getIssueService();
            ProjectManager projectManager = ComponentAccessor.getProjectManager();
            Project project = projectManager.getProjectObjByKey(projectKey);

            if (project == null) {
                resp.getWriter().write("<h3>Project not found: " + projectKey + "</h3>");
                return;
            }

            int deleted = 0;

            if ("deleteAll".equals(action)) {
                Collection<Long> issueIds = issueManager.getIssueIdsForProject(project.getId());
                for (Long issueId : issueIds) {
                    IssueService.DeleteValidationResult result = issueService.validateDelete(user, issueId);
                    if (result.isValid()) {
                        issueService.delete(user, result);
                        deleted++;
                    }
                }
            } else if ("deleteSelected".equals(action)) {
                String[] selectedIds = req.getParameterValues("issueId");
                if (selectedIds != null) {
                    for (String idStr : selectedIds) {
                        try {
                            Long issueId = Long.parseLong(idStr);
                            IssueService.DeleteValidationResult result = issueService.validateDelete(user, issueId);
                            if (result.isValid()) {
                                issueService.delete(user, result);
                                deleted++;
                            }
                        } catch (NumberFormatException ignored) {}
                    }
                }
            }

            resp.getWriter().write(
                "<html><body style='font-family:Arial;padding:20px'>" +
                "<h2>Done!</h2>" +
                "<p>Deleted <strong>" + deleted + "</strong> issue(s) from project <strong>" + projectKey + "</strong></p>" +
                "<a href='/plugins/servlet/bulk-delete-page?projectKey=" + projectKey + "'>Go Back</a>" +
                "</body></html>"
            );

        } catch (Exception e) {
            resp.getWriter().write("<h3>Error: " + e.getMessage() + "</h3>");
        }
    }
}
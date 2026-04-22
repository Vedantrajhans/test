package com.example.jira.bulkdelete.rest;

import com.atlassian.jira.bc.issue.IssueService;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.project.ProjectManager;
import com.atlassian.jira.user.ApplicationUser;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;
import java.util.Collection;

@Path("/delete")
public class DeleteProjectResource {

    @POST
    @Path("/{projectKey}")
    public Response deleteIssues(@PathParam("projectKey") String projectKey) {
        try {
            ApplicationUser user = ComponentAccessor
                    .getJiraAuthenticationContext().getLoggedInUser();

            if (user == null) {
                return Response.status(Response.Status.UNAUTHORIZED)
                        .entity("Not logged in").build();
            }

            ProjectManager projectManager = ComponentAccessor.getProjectManager();
            Project project = projectManager.getProjectObjByKey(projectKey.toUpperCase());

            if (project == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity("Project not found: " + projectKey).build();
            }

            IssueManager issueManager = ComponentAccessor.getIssueManager();
            IssueService issueService = ComponentAccessor.getIssueService();

            Collection<Long> issueIds = issueManager.getIssueIdsForProject(project.getId());
            int deleted = 0;

            for (Long issueId : issueIds) {
                IssueService.DeleteValidationResult result =
                        issueService.validateDelete(user, issueId);
                if (result.isValid()) {
                    issueService.delete(user, result);
                    deleted++;
                }
            }

            return Response.ok("Deleted " + deleted + " issues from " + projectKey).build();

        } catch (Exception e) {
            return Response.serverError().entity("Error: " + e.getMessage()).build();
        }
    }
}
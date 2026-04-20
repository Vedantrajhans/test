package com.vedant.concert_platform.service;

import com.vedant.concert_platform.entity.Concert;
import com.vedant.concert_platform.entity.TicketBooking;
import com.vedant.concert_platform.entity.TicketType;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EmailService {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;

    @Value("${app.mail.from:no-reply@concert.local}")
    private String mailFrom;

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy 'at' h:mm a");

    @Async
    public void sendBookingConfirmation(TicketBooking booking) {
        try {
            Concert concert = booking.getConcert();
            TicketType tt = booking.getTicketType();
            String attendeeName = booking.getUser().getFirstName() + " " + booking.getUser().getLastName();
            String qrDataUrl = generateQrDataUrl(booking.getQrCode() != null ? booking.getQrCode() : booking.getBookingReference());

            String html = buildBookingEmail(attendeeName, concert, tt, booking, qrDataUrl, false);
            sendHtmlEmail(booking.getUser().getEmail(), "🎵 Booking Confirmed – " + concert.getTitle(), html);
        } catch (Exception ex) {
            log.warn("Failed to send booking confirmation email: {}", ex.getMessage());
        }
    }

    @Async
    public void sendConcertUpdateNotification(List<TicketBooking> bookings, Concert concert, String changeDescription) {
        for (TicketBooking booking : bookings) {
            try {
                String attendeeName = booking.getUser().getFirstName() + " " + booking.getUser().getLastName();
                String html = buildUpdateEmail(attendeeName, concert, booking, changeDescription);
                sendHtmlEmail(booking.getUser().getEmail(), "⚠️ Concert Update – " + concert.getTitle(), html);
            } catch (Exception ex) {
                log.warn("Failed to send concert update email to {}: {}", booking.getUser().getEmail(), ex.getMessage());
            }
        }
    }

    private void sendHtmlEmail(String to, String subject, String html) throws MessagingException {
        MimeMessage msg = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(msg, true, "UTF-8");
        helper.setFrom(mailFrom);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(html, true);
        mailSender.send(msg);
    }

    private String buildBookingEmail(String attendeeName, Concert concert, TicketType tt,
                                     TicketBooking booking, String qrDataUrl, boolean isUpdate) {
        String concertDate = concert.getDateTime() != null ? concert.getDateTime().format(FMT) : "TBA";
        String endDate = concert.getEndTime() != null ? concert.getEndTime().format(FMT) : "";
        String venue = "";
        if (concert.getVenue() != null) {
            venue = (concert.getVenue().getAddress() != null ? concert.getVenue().getAddress() + ", " : "")
                    + (concert.getVenue().getCity() != null ? concert.getVenue().getCity() : "");
        }

        return """
            <!DOCTYPE html>
            <html>
            <head><meta charset="UTF-8"/></head>
            <body style="background:#0f0f0f;color:#e0e0e0;font-family:Arial,sans-serif;margin:0;padding:0;">
              <div style="max-width:580px;margin:30px auto;background:#1a1a1a;border-radius:12px;overflow:hidden;border:1px solid #2a2a2a;">
                <div style="background:linear-gradient(135deg,#ff5c35,#9b59b6);padding:30px 24px;text-align:center;">
                  <div style="font-size:28px;margin-bottom:8px;">🎵</div>
                  <h1 style="color:white;margin:0;font-size:22px;">Booking Confirmed!</h1>
                  <p style="color:rgba(255,255,255,0.85);margin:8px 0 0;font-size:14px;">Your tickets for <strong>%s</strong> are ready.</p>
                </div>
                <div style="padding:24px;">
                  <p style="margin:0 0 20px;color:#aaa;font-size:14px;">Hi %s,</p>
                  <div style="background:#111;border-radius:10px;padding:20px;margin-bottom:20px;border:1px solid #2a2a2a;">
                    <h2 style="margin:0 0 16px;font-size:18px;color:#ff5c35;">%s</h2>
                    <table style="width:100%%;border-collapse:collapse;font-size:13px;">
                      <tr><td style="padding:6px 0;color:#888;">📅 Date</td><td style="padding:6px 0;color:#e0e0e0;text-align:right;">%s</td></tr>
                      %s
                      %s
                      <tr><td style="padding:6px 0;color:#888;">🎫 Ticket Type</td><td style="padding:6px 0;color:#e0e0e0;text-align:right;">%s</td></tr>
                      <tr><td style="padding:6px 0;color:#888;">🔢 Quantity</td><td style="padding:6px 0;color:#e0e0e0;text-align:right;">%d</td></tr>
                      <tr style="border-top:1px solid #2a2a2a;"><td style="padding:10px 0;color:#888;font-weight:bold;">💰 Total Paid</td><td style="padding:10px 0;color:#4ade80;text-align:right;font-weight:bold;">₹%.2f</td></tr>
                      <tr><td style="padding:6px 0;color:#888;">🔖 Reference</td><td style="padding:6px 0;color:#e0e0e0;text-align:right;font-family:monospace;font-size:12px;">%s</td></tr>
                    </table>
                  </div>
                  <div style="text-align:center;margin-bottom:20px;">
                    <p style="color:#888;font-size:13px;margin-bottom:12px;">Scan this QR code at the venue entrance:</p>
                    %s
                    <p style="color:#555;font-size:11px;margin-top:8px;font-family:monospace;">%s</p>
                  </div>
                  <div style="background:#1e1e1e;border-radius:8px;padding:14px;font-size:12px;color:#666;text-align:center;">
                    Keep this email safe. Present your QR code or booking reference at the gate.
                  </div>
                </div>
                <div style="text-align:center;padding:16px;color:#444;font-size:12px;border-top:1px solid #2a2a2a;">— The Festiva Team 🎵</div>
              </div>
            </body>
            </html>
            """.formatted(
                concert.getTitle(), attendeeName, concert.getTitle(), concertDate,
                endDate.isEmpty() ? "" : "<tr><td style='padding:6px 0;color:#888;'>⏱ End Time</td><td style='padding:6px 0;color:#e0e0e0;text-align:right;'>"+endDate+"</td></tr>",
                venue.isEmpty() ? "" : "<tr><td style='padding:6px 0;color:#888;'>📍 Venue</td><td style='padding:6px 0;color:#e0e0e0;text-align:right;'>"+venue+"</td></tr>",
                tt.getName(), booking.getQuantity(),
                booking.getTotalAmount() != null ? booking.getTotalAmount().doubleValue() : 0.0,
                booking.getBookingReference() != null ? booking.getBookingReference() : "Pending",
                qrDataUrl,
                booking.getQrCode() != null ? booking.getQrCode() : ""
        );
    }

    private String buildUpdateEmail(String attendeeName, Concert concert, TicketBooking booking, String changeDescription) {
        String concertDate = concert.getDateTime() != null ? concert.getDateTime().format(FMT) : "TBA";
        String venue = "";
        if (concert.getVenue() != null) {
            venue = (concert.getVenue().getAddress() != null ? concert.getVenue().getAddress() + ", " : "")
                    + (concert.getVenue().getCity() != null ? concert.getVenue().getCity() : "");
        }
        return """
            <!DOCTYPE html>
            <html>
            <head><meta charset="UTF-8"/></head>
            <body style="background:#0f0f0f;color:#e0e0e0;font-family:Arial,sans-serif;margin:0;padding:0;">
              <div style="max-width:580px;margin:30px auto;background:#1a1a1a;border-radius:12px;overflow:hidden;border:1px solid #2a2a2a;">
                <div style="background:linear-gradient(135deg,#f59e0b,#ef4444);padding:28px 24px;text-align:center;">
                  <div style="font-size:28px;margin-bottom:8px;">⚠️</div>
                  <h1 style="color:white;margin:0;font-size:22px;">Concert Update</h1>
                  <p style="color:rgba(255,255,255,0.85);margin:8px 0 0;font-size:14px;"><strong>%s</strong> has been updated</p>
                </div>
                <div style="padding:24px;">
                  <p style="margin:0 0 16px;color:#aaa;font-size:14px;">Hi %s,</p>
                  <div style="background:#2a1a0a;border-radius:8px;padding:14px;margin-bottom:20px;border:1px solid #7c4a00;color:#f59e0b;font-size:13px;">
                    <strong>What changed:</strong> %s
                  </div>
                  <div style="background:#111;border-radius:10px;padding:20px;border:1px solid #2a2a2a;">
                    <h2 style="margin:0 0 14px;font-size:17px;color:#ff5c35;">Updated Concert Details</h2>
                    <table style="width:100%%;border-collapse:collapse;font-size:13px;">
                      <tr><td style="padding:6px 0;color:#888;">📅 New Date</td><td style="padding:6px 0;color:#e0e0e0;text-align:right;">%s</td></tr>
                      %s
                      <tr><td style="padding:6px 0;color:#888;">🔖 Your Ref</td><td style="padding:6px 0;color:#e0e0e0;text-align:right;font-family:monospace;font-size:12px;">%s</td></tr>
                    </table>
                  </div>
                  <p style="margin:20px 0 0;font-size:12px;color:#555;">Your existing tickets remain valid. Contact support if you need assistance.</p>
                </div>
                <div style="text-align:center;padding:16px;color:#444;font-size:12px;border-top:1px solid #2a2a2a;">— The Festiva Team 🎵</div>
              </div>
            </body>
            </html>
            """.formatted(
                concert.getTitle(), attendeeName, changeDescription, concertDate,
                venue.isEmpty() ? "" : "<tr><td style='padding:6px 0;color:#888;'>📍 Venue</td><td style='padding:6px 0;color:#e0e0e0;text-align:right;'>"+venue+"</td></tr>",
                booking.getBookingReference() != null ? booking.getBookingReference() : "Pending"
        );
    }

    // Generates a simple SVG QR-like image as data URL for email embedding
    private String generateQrDataUrl(String data) {
        if (data == null) data = "FESTIVA";
        // Simple SVG that looks like a QR code placeholder — readable by eye, scannable as reference
        String svg = "<svg xmlns='http://www.w3.org/2000/svg' width='120' height='120' viewBox='0 0 120 120'>" +
                "<rect width='120' height='120' fill='white'/>" +
                "<rect x='10' y='10' width='40' height='40' fill='none' stroke='black' stroke-width='4'/>" +
                "<rect x='18' y='18' width='24' height='24' fill='black'/>" +
                "<rect x='70' y='10' width='40' height='40' fill='none' stroke='black' stroke-width='4'/>" +
                "<rect x='78' y='18' width='24' height='24' fill='black'/>" +
                "<rect x='10' y='70' width='40' height='40' fill='none' stroke='black' stroke-width='4'/>" +
                "<rect x='18' y='78' width='24' height='24' fill='black'/>" +
                "<text x='60' y='68' text-anchor='middle' font-size='6' fill='black'>" + 
                (data.length() > 20 ? data.substring(0, 20) : data) + "</text>" +
                "</svg>";
        String encoded = java.util.Base64.getEncoder().encodeToString(svg.getBytes());
        return "<img src='data:image/svg+xml;base64," + encoded + "' width='120' height='120' style='border:4px solid white;border-radius:8px;' alt='QR Code'/>";
    }
}

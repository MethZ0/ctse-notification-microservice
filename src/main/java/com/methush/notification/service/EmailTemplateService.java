package com.methush.notification.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Builds HTML email bodies for each notification type.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailTemplateService {

    @Value("${spring.application.name:Notification Service}")
    private String appName;

    /**
     * HTML email for a new order confirmation.
     *
     * @param orderId the custom order ID (e.g. "ORD#0001")
     * @return HTML string
     */
    public String buildOrderConfirmationHtml(String orderId) {
        return "<!DOCTYPE html>" +
               "<html lang=\"en\">" +
               "<head><meta charset=\"UTF-8\"><meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">" +
               "<style>" +
               "  body { font-family: Arial, sans-serif; background-color: #f4f4f4; margin: 0; padding: 0; }" +
               "  .email-container { max-width: 600px; margin: 40px auto; background-color: #ffffff; border-radius: 8px; overflow: hidden; box-shadow: 0 2px 8px rgba(0,0,0,0.1); }" +
               "  .header { background-color: #2ecc71; padding: 30px 40px; text-align: center; }" +
               "  .header h1 { color: #ffffff; margin: 0; font-size: 26px; letter-spacing: 1px; }" +
               "  .body { padding: 40px; color: #333333; }" +
               "  .body h2 { color: #2ecc71; font-size: 20px; }" +
               "  .order-box { background-color: #f0faf4; border: 1px solid #2ecc71; border-radius: 6px; padding: 20px; margin: 24px 0; text-align: center; }" +
               "  .order-box p { margin: 0; font-size: 14px; color: #555555; }" +
               "  .order-box span { font-size: 28px; font-weight: bold; color: #27ae60; display: block; margin-top: 8px; }" +
               "  .steps { margin: 24px 0; padding-left: 0; list-style: none; }" +
               "  .steps li { padding: 10px 0; border-bottom: 1px solid #eeeeee; color: #555555; font-size: 14px; }" +
               "  .steps li:last-child { border-bottom: none; }" +
               "  .steps li::before { content: '✓ '; color: #2ecc71; font-weight: bold; }" +
               "  .footer { background-color: #f9f9f9; padding: 20px 40px; text-align: center; font-size: 12px; color: #aaaaaa; border-top: 1px solid #eeeeee; }" +
               "</style></head>" +
               "<body>" +
               "  <div class=\"email-container\">" +
               "    <div class=\"header\"><h1>✅ Order Confirmed!</h1></div>" +
               "    <div class=\"body\">" +
               "      <h2>Thank you for your order!</h2>" +
               "      <p>We've received your order and it's now being processed. Here are your order details:</p>" +
               "      <div class=\"order-box\">" +
               "        <p>Your Order ID</p>" +
               "        <span>" + orderId + "</span>" +
               "      </div>" +
               "      <p>Here's what happens next:</p>" +
               "      <ul class=\"steps\">" +
               "        <li>Your order is being reviewed and confirmed</li>" +
               "        <li>Items will be picked and packed from our warehouse</li>" +
               "        <li>Your package will be shipped to your delivery address</li>" +
               "        <li>You will receive a status update email at each step</li>" +
               "      </ul>" +
               "      <p>If you have any questions, please contact our support team.</p>" +
               "    </div>" +
               "    <div class=\"footer\">" +
               "      <p>This is an automated email. Please do not reply directly to this email.</p>" +
               "      <p>&copy; 2026 E-Commerce Platform. All rights reserved.</p>" +
               "    </div>" +
               "  </div>" +
               "</body></html>";
    }

    /**
     * HTML email for an order status update.
     *
     * @param orderId     the custom order ID (e.g. "ORD#0001")
     * @param orderStatus the new order status (e.g. "Shipped", "Delivered")
     * @return HTML string
     */
    public String buildOrderStatusUpdateHtml(String orderId, String orderStatus) {
        String statusColor = resolveStatusColor(orderStatus);
        String statusIcon = resolveStatusIcon(orderStatus);

        return "<!DOCTYPE html>" +
               "<html lang=\"en\">" +
               "<head><meta charset=\"UTF-8\"><meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">" +
               "<style>" +
               "  body { font-family: Arial, sans-serif; background-color: #f4f4f4; margin: 0; padding: 0; }" +
               "  .email-container { max-width: 600px; margin: 40px auto; background-color: #ffffff; border-radius: 8px; overflow: hidden; box-shadow: 0 2px 8px rgba(0,0,0,0.1); }" +
               "  .header { background-color: " + statusColor + "; padding: 30px 40px; text-align: center; }" +
               "  .header h1 { color: #ffffff; margin: 0; font-size: 26px; letter-spacing: 1px; }" +
               "  .body { padding: 40px; color: #333333; }" +
               "  .body h2 { color: " + statusColor + "; font-size: 20px; }" +
               "  .order-box { background-color: #f8f9fa; border: 1px solid #dee2e6; border-radius: 6px; padding: 20px; margin: 24px 0; }" +
               "  .order-box table { width: 100%; border-collapse: collapse; }" +
               "  .order-box td { padding: 10px 0; font-size: 14px; color: #555555; border-bottom: 1px solid #eeeeee; }" +
               "  .order-box td:last-child { text-align: right; font-weight: bold; color: #333333; }" +
               "  .order-box tr:last-child td { border-bottom: none; }" +
               "  .status-badge { display: inline-block; padding: 6px 16px; border-radius: 20px; background-color: " + statusColor + "; color: #ffffff; font-weight: bold; font-size: 14px; }" +
               "  .footer { background-color: #f9f9f9; padding: 20px 40px; text-align: center; font-size: 12px; color: #aaaaaa; border-top: 1px solid #eeeeee; }" +
               "</style></head>" +
               "<body>" +
               "  <div class=\"email-container\">" +
               "    <div class=\"header\"><h1>" + statusIcon + " Order Status Updated</h1></div>" +
               "    <div class=\"body\">" +
               "      <h2>Your order status has changed!</h2>" +
               "      <p>We're writing to let you know that your order status has been updated.</p>" +
               "      <div class=\"order-box\">" +
               "        <table>" +
               "          <tr><td>Order ID</td><td>" + orderId + "</td></tr>" +
               "          <tr><td>New Status</td><td><span class=\"status-badge\">" + orderStatus + "</span></td></tr>" +
               "        </table>" +
               "      </div>" +
               "      <p>We'll continue to keep you updated as your order progresses. Thank you for shopping with us!</p>" +
               "    </div>" +
               "    <div class=\"footer\">" +
               "      <p>This is an automated email. Please do not reply directly to this email.</p>" +
               "      <p>&copy; 2026 E-Commerce Platform. All rights reserved.</p>" +
               "    </div>" +
               "  </div>" +
               "</body></html>";
    }

    /** Returns a hex color based on the order status. */
    private String resolveStatusColor(String status) {
        if (status == null) return "#3498db";
        return switch (status.toLowerCase()) {
            case "pending"    -> "#f39c12";
            case "confirmed"  -> "#3498db";
            case "processing" -> "#9b59b6";
            case "shipped"    -> "#2980b9";
            case "delivered"  -> "#27ae60";
            case "cancelled"  -> "#e74c3c";
            default           -> "#3498db";
        };
    }

    /** Returns an emoji icon based on the order status. */
    private String resolveStatusIcon(String status) {
        if (status == null) return "📦";
        return switch (status.toLowerCase()) {
            case "pending"    -> "⏳";
            case "confirmed"  -> "✅";
            case "processing" -> "⚙️";
            case "shipped"    -> "🚚";
            case "delivered"  -> "🎉";
            case "cancelled"  -> "❌";
            default           -> "📦";
        };
    }
}

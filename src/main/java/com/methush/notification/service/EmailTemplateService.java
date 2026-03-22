package com.methush.notification.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

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
     * Rich HTML email for a new order confirmation with items, total and delivery address.
     *
     * @param orderId         the human-readable order ID (e.g. "ORD#0003")
     * @param items           list of order items (each has productId, quantity, price)
     * @param total           order total amount
     * @param deliveryAddress delivery address string
     * @return HTML string
     */
    public String buildOrderConfirmationHtml(String orderId,
                                              List<Map<String, Object>> items,
                                              double total,
                                              String deliveryAddress) {
        // Build the items rows HTML
        StringBuilder itemRows = new StringBuilder();
        if (items != null && !items.isEmpty()) {
            for (Map<String, Object> item : items) {
                String productId = item.get("productId") != null ? item.get("productId").toString() : "-";
                Object qty = item.get("quantity");
                Object price = item.get("price");
                double lineTotal = (qty != null ? ((Number) qty).doubleValue() : 0)
                                 * (price != null ? ((Number) price).doubleValue() : 0);
                itemRows.append("<tr>")
                        .append("<td>").append(productId, 0, Math.min(8, productId.length())).append("...").append("</td>")
                        .append("<td style='text-align:center'>").append(qty != null ? qty : 0).append("</td>")
                        .append("<td style='text-align:right'>LKR ").append(String.format("%.2f", price != null ? ((Number) price).doubleValue() : 0)).append("</td>")
                        .append("<td style='text-align:right'>LKR ").append(String.format("%.2f", lineTotal)).append("</td>")
                        .append("</tr>");
            }
        } else {
            itemRows.append("<tr><td colspan='4' style='text-align:center;color:#aaa'>No item details available</td></tr>");
        }

        String addressHtml = (deliveryAddress != null && !deliveryAddress.isBlank())
                ? "<p style='margin:8px 0;font-size:14px;color:#555'><b>Deliver to:</b> " + deliveryAddress + "</p>"
                : "";

        return "<!DOCTYPE html>" +
               "<html lang=\"en\">" +
               "<head><meta charset=\"UTF-8\"><meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">" +
               "<style>" +
               "  body { font-family: Arial, sans-serif; background-color: #f4f4f4; margin: 0; padding: 0; }" +
               "  .email-container { max-width: 620px; margin: 40px auto; background-color: #ffffff; border-radius: 8px; overflow: hidden; box-shadow: 0 2px 8px rgba(0,0,0,0.1); }" +
               "  .header { background-color: #2ecc71; padding: 28px 40px; text-align: center; }" +
               "  .header h1 { color: #ffffff; margin: 0; font-size: 24px; }" +
               "  .body { padding: 36px 40px; color: #333333; }" +
               "  .body h2 { color: #2ecc71; font-size: 18px; margin-top: 0; }" +
               "  .order-id-box { background:#f0faf4; border:1px solid #2ecc71; border-radius:6px; padding:16px; text-align:center; margin:20px 0; }" +
               "  .order-id-box p { margin:0; font-size:13px; color:#555; }" +
               "  .order-id-box span { font-size:26px; font-weight:bold; color:#27ae60; display:block; margin-top:6px; }" +
               "  .items-table { width:100%; border-collapse:collapse; margin:20px 0; font-size:13px; }" +
               "  .items-table th { background:#f8f8f8; padding:8px 10px; text-align:left; border-bottom:2px solid #eee; color:#666; }" +
               "  .items-table td { padding:8px 10px; border-bottom:1px solid #f0f0f0; color:#444; }" +
               "  .total-row { font-weight:bold; font-size:15px; }" +
               "  .total-row td { border-top:2px solid #2ecc71 !important; padding-top:12px !important; color:#27ae60; }" +
               "  .footer { background-color:#f9f9f9; padding:18px 40px; text-align:center; font-size:11px; color:#aaa; border-top:1px solid #eee; }" +
               "</style></head>" +
               "<body>" +
               "  <div class=\"email-container\">" +
               "    <div class=\"header\"><h1>✅ Order Confirmed!</h1></div>" +
               "    <div class=\"body\">" +
               "      <h2>Thank you for your order!</h2>" +
               "      <p>Your order has been received and is now being processed.</p>" +
               "      <div class=\"order-id-box\"><p>Order ID</p><span>" + orderId + "</span></div>" +
               addressHtml +
               "      <table class=\"items-table\">" +
               "        <thead><tr>" +
               "          <th>Product</th><th style='text-align:center'>Qty</th><th style='text-align:right'>Unit Price</th><th style='text-align:right'>Subtotal</th>" +
               "        </tr></thead>" +
               "        <tbody>" + itemRows + "</tbody>" +
               "        <tfoot><tr class=\"total-row\">" +
               "          <td colspan='3'>Total</td><td style='text-align:right'>LKR " + String.format("%.2f", total) + "</td>" +
               "        </tr></tfoot>" +
               "      </table>" +
               "      <p style='font-size:13px;color:#666'>If you have any questions, please contact our support team.</p>" +
               "    </div>" +
               "    <div class=\"footer\">" +
               "      <p>This is an automated email. Please do not reply.</p>" +
               "      <p>&copy; 2026 E-Commerce Platform. All rights reserved.</p>" +
               "    </div>" +
               "  </div>" +
               "</body></html>";
    }

    /**
     * HTML email for an order status update — now includes order total.
     *
     * @param orderId     the human-readable order ID (e.g. "ORD#0003")
     * @param orderStatus the new order status
     * @param total       the order total
     * @return HTML string
     */
    public String buildOrderStatusUpdateHtml(String orderId, String orderStatus, double total) {
        String statusColor = resolveStatusColor(orderStatus);
        String statusIcon  = resolveStatusIcon(orderStatus);

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
               "          <tr><td>Order Total</td><td>LKR " + String.format("%.2f", total) + "</td></tr>" +
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

    /**
     * HTML email for a low stock alert sent to admin users.
     *
     * @param alertMessage human-readable message from the inventory service
     * @param productId    the product's MongoDB ID
     * @return HTML string
     */
    public String buildLowStockAlertHtml(String alertMessage, String productId) {
        return "<!DOCTYPE html>" +
               "<html lang=\"en\">" +
               "<head><meta charset=\"UTF-8\"><meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">" +
               "<style>" +
               "  body { font-family: Arial, sans-serif; background-color: #f4f4f4; margin: 0; padding: 0; }" +
               "  .email-container { max-width: 600px; margin: 40px auto; background-color: #ffffff; border-radius: 8px; overflow: hidden; box-shadow: 0 2px 8px rgba(0,0,0,0.1); }" +
               "  .header { background-color: #e67e22; padding: 30px 40px; text-align: center; }" +
               "  .header h1 { color: #ffffff; margin: 0; font-size: 26px; letter-spacing: 1px; }" +
               "  .body { padding: 40px; color: #333333; }" +
               "  .body h2 { color: #e67e22; font-size: 20px; }" +
               "  .alert-box { background-color: #fef9f0; border: 2px solid #e67e22; border-radius: 6px; padding: 20px; margin: 24px 0; }" +
               "  .alert-box p { margin: 0 0 8px; font-size: 15px; color: #555555; }" +
               "  .alert-box .message { font-size: 16px; font-weight: bold; color: #c0392b; }" +
               "  .product-id { font-size: 12px; color: #999999; margin-top: 10px; word-break: break-all; }" +
               "  .action-note { background-color: #fff3cd; border-left: 4px solid #f0ad4e; padding: 12px 16px; border-radius: 4px; font-size: 14px; color: #856404; margin: 16px 0; }" +
               "  .footer { background-color: #f9f9f9; padding: 20px 40px; text-align: center; font-size: 12px; color: #aaaaaa; border-top: 1px solid #eeeeee; }" +
               "</style></head>" +
               "<body>" +
               "  <div class=\"email-container\">" +
               "    <div class=\"header\"><h1>⚠️ Low Stock Alert</h1></div>" +
               "    <div class=\"body\">" +
               "      <h2>Inventory Action Required!</h2>" +
               "      <p>This is an automated alert from the inventory system. A product has dropped below the minimum stock threshold.</p>" +
               "      <div class=\"alert-box\">" +
               "        <p class=\"message\">" + alertMessage + "</p>" +
               (productId != null ? "        <p class=\"product-id\">Product ID: " + productId + "</p>" : "") +
               "      </div>" +
               "      <div class=\"action-note\">📋 Please log in to the admin panel and restock this product as soon as possible to avoid order fulfilment issues.</div>" +
               "    </div>" +
               "    <div class=\"footer\">" +
               "      <p>This is an automated alert from the Inventory Management System.</p>" +
               "      <p>&copy; 2026 E-Commerce Platform. All rights reserved.</p>" +
               "    </div>" +
               "  </div>" +
               "</body></html>";
    }
}

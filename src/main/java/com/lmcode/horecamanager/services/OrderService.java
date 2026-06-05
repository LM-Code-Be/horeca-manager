package com.lmcode.horecamanager.services;

import com.lmcode.horecamanager.models.Order;
import com.lmcode.horecamanager.models.OrderItem;
import com.lmcode.horecamanager.models.Payment;
import com.lmcode.horecamanager.models.Product;
import com.lmcode.horecamanager.models.RestaurantTable;
import com.lmcode.horecamanager.models.Ticket;
import com.lmcode.horecamanager.repositories.CashSessionRepository;
import com.lmcode.horecamanager.repositories.OrderRepository;
import com.lmcode.horecamanager.repositories.PaymentRepository;
import com.lmcode.horecamanager.repositories.ProductRepository;
import com.lmcode.horecamanager.repositories.TableRepository;
import com.lmcode.horecamanager.utils.MoneyUtils;

import java.time.LocalDateTime;
import java.util.List;

public class OrderService {
    private final OrderRepository orderRepository = new OrderRepository();
    private final PaymentRepository paymentRepository = new PaymentRepository();
    private final ProductRepository productRepository = new ProductRepository();
    private final CashSessionRepository cashSessionRepository = new CashSessionRepository();
    private final TableRepository tableRepository = new TableRepository();
    private final TicketService ticketService = new TicketService();
    private final ValidationService validationService = new ValidationService();

    public int createOrder(Integer tableId, String orderType) {
        if (tableId != null) {
            RestaurantTable table = tableRepository.findById(tableId);
            if (table == null) {
                throw new IllegalArgumentException("Table introuvable.");
            }
            if ("INDISPONIBLE".equals(table.status())) {
                throw new IllegalArgumentException("Table indisponible.");
            }
            if ("A_NETTOYER".equals(table.status())) {
                throw new IllegalArgumentException("Table a nettoyer avant une nouvelle commande.");
            }
            Order existing = orderRepository.findOpenByTableId(tableId);
            if (existing != null) {
                orderRepository.resume(existing.id());
                tableRepository.updateStatus(tableId, "OCCUPEE");
                return existing.id();
            }
        }
        int orderId = orderRepository.create(tableId, orderType);
        if (tableId != null) {
            tableRepository.updateStatus(tableId, "OCCUPEE");
        }
        return orderId;
    }

    public void addProduct(int orderId, Product product, int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantite invalide.");
        }
        Product current = productRepository.findById(product.id());
        if (current == null) {
            throw new IllegalArgumentException("Produit introuvable.");
        }
        if (!current.available()) {
            throw new IllegalArgumentException("Produit indisponible.");
        }
        int alreadyInOrder = orderRepository.findItems(orderId)
                .stream()
                .filter(item -> item.productId() == current.id())
                .mapToInt(OrderItem::quantity)
                .sum();
        validateStock(current, alreadyInOrder + quantity, orderId);
        orderRepository.addItem(orderId, current, quantity);
    }

    public void updateItemQuantity(int itemId, int quantity) {
        OrderItem item = orderRepository.findItemById(itemId);
        if (item == null) {
            throw new IllegalArgumentException("Ligne de commande introuvable.");
        }
        if (quantity > 0) {
            Product product = productRepository.findById(item.productId());
            validateStock(product, quantity, item.orderId());
        }
        orderRepository.updateItemQuantity(itemId, quantity);
    }

    public void applyDiscount(int orderId, double discount) {
        orderRepository.applyDiscount(orderId, discount);
    }

    public void cancelOrder(int orderId) {
        Order order = orderRepository.findById(orderId);
        if (order == null) {
            throw new IllegalArgumentException("Commande introuvable.");
        }
        orderRepository.cancel(orderId);
        if (order.tableId() != null && orderRepository.findOpenByTableId(order.tableId()) == null) {
            tableRepository.updateStatus(order.tableId(), "LIBRE");
        }
    }

    public Ticket payOrder(int orderId, String method, double amountReceived) {
        if (!cashSessionRepository.isOpen()) {
            throw new IllegalArgumentException("Caisse fermee.");
        }
        Order order = orderRepository.findById(orderId);
        if (order == null) {
            throw new IllegalArgumentException("Commande introuvable.");
        }
        List<OrderItem> items = orderRepository.findItems(orderId);
        validationService.validateOrderCanBePaid(items);
        if ("ESPECES".equals(method)) {
            validationService.validateCashPayment(order.total(), amountReceived);
        }
        for (OrderItem item : items) {
            validateStock(productRepository.findById(item.productId()), item.quantity(), orderId);
        }
        double received = "ESPECES".equals(method) ? amountReceived : order.total();
        double change = "ESPECES".equals(method) ? MoneyUtils.round(received - order.total()) : 0;
        Payment payment = new Payment(0, orderId, method, order.total(), received, change, LocalDateTime.now());
        paymentRepository.save(payment);
        for (OrderItem item : items) {
            productRepository.decreaseStock(item.productId(), item.quantity());
        }
        orderRepository.close(orderId);
        cashSessionRepository.increaseExpected(order.total());
        if (order.tableId() != null) {
            tableRepository.updateStatus(order.tableId(), "A_NETTOYER");
        }
        Order closed = orderRepository.findById(orderId);
        return ticketService.generateAndSave(closed, items, payment);
    }

    private void validateStock(Product product, int requestedQuantity, int orderId) {
        if (product == null) {
            throw new IllegalArgumentException("Produit introuvable.");
        }
        if (product.stockQuantity() == null) {
            return;
        }
        if (product.stockQuantity() <= 0) {
            productRepository.updateAvailability(product.id(), false);
            throw new IllegalArgumentException(product.name() + " est en rupture. Reapprovisionnez le produit.");
        }
        int reservedElsewhere = orderRepository.reservedQuantityForProduct(product.id(), orderId);
        int available = product.stockQuantity() - reservedElsewhere;
        if (requestedQuantity > available) {
            throw new IllegalArgumentException("Stock insuffisant pour " + product.name() + ". Disponible: " + Math.max(0, available) + ".");
        }
    }
}

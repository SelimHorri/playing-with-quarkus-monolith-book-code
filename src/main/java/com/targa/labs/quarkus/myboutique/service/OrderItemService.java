package com.targa.labs.quarkus.myboutique.service;

import com.targa.labs.quarkus.myboutique.domain.Order;
import com.targa.labs.quarkus.myboutique.domain.OrderItem;
import com.targa.labs.quarkus.myboutique.domain.Product;
import com.targa.labs.quarkus.myboutique.repository.OrderItemRepository;
import com.targa.labs.quarkus.myboutique.repository.OrderRepository;
import com.targa.labs.quarkus.myboutique.repository.ProductRepository;
import com.targa.labs.quarkus.myboutique.web.dto.OrderItemDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.transaction.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@ApplicationScoped
@Transactional
public class OrderItemService {

    private final Logger log = LoggerFactory.getLogger(OrderItemService.class);

    private final OrderItemRepository orderItemRepository;
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;

    public OrderItemService(OrderItemRepository orderItemRepository,
                            OrderRepository orderRepository,
                            ProductRepository productRepository) {
        this.orderItemRepository = orderItemRepository;
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
    }

    public static OrderItemDto mapToDto(OrderItem orderItem) {
        return new OrderItemDto(
                orderItem.getId(),
                orderItem.getQuantity(),
                orderItem.getProduct().getId(),
                orderItem.getOrder().getId()
        );
    }

    @Transactional
    public OrderItemDto findById(Long id) {
        log.debug("Request to get OrderItem : {}", id);
        return this.orderItemRepository.findById(id).map(OrderItemService::mapToDto).orElse(null);
    }

    public OrderItemDto create(OrderItemDto orderItemDto) {
        log.debug("Request to create OrderItem : {}", orderItemDto);
        Order order =
                this.orderRepository
                        .findById(orderItemDto.getOrderId())
                        .orElseThrow(() -> new IllegalStateException("The Order does not exist!"));

        Product product =
                this.productRepository
                        .findById(orderItemDto.getProductId())
                        .orElseThrow(() -> new IllegalStateException("The Product does not exist!"));

        OrderItem orderItem = this.orderItemRepository.save(
                new OrderItem(
                        orderItemDto.getQuantity(),
                        product,
                        order
                ));

        order.setPrice(
                order.getPrice().add(orderItem.getProduct().getPrice())
        );

        this.orderRepository.save(order);

        return mapToDto(orderItem);
    }

    public void delete(Long id) {
        log.debug("Request to delete OrderItem : {}", id);

        OrderItem orderItem = this.orderItemRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("The OrderItem does not exist!"));

        Order order = orderItem.getOrder();
        order.setPrice(
                order.getPrice().subtract(orderItem.getProduct().getPrice())
        );

        this.orderItemRepository.deleteById(id);

        order.getOrderItems().remove(orderItem);

        this.orderRepository.save(order);
    }

    public List<OrderItemDto> findByOrderId(Long id) {
        log.debug("Request to get all OrderItems of OrderId {}", id);
        return this.orderItemRepository.findAllByOrderId(id)
                .stream()
                .map(OrderItemService::mapToDto)
                .collect(Collectors.toList());
    }
}

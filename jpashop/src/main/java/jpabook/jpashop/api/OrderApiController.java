package jpabook.jpashop.api;

import jpabook.jpashop.domain.Address;
import jpabook.jpashop.domain.Order;
import jpabook.jpashop.domain.OrderItem;
import jpabook.jpashop.domain.OrderStatus;
import jpabook.jpashop.repository.OrderRepository;
import jpabook.jpashop.repository.OrderSearch;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
public class OrderApiController {

    private final OrderRepository orderRepository;

    @GetMapping("/api/v1/orders")
    public List<Order> ordersV1() {
        List<Order> all = orderRepository.findAllByString(new OrderSearch());
        for (Order order : all) {
            order.getMember().getName(); //강제 초기화
            order.getDelivery().getAddress();  //강제 초기화

            List<OrderItem> orderItems = order.getOrderItems();
            orderItems.stream().forEach(o -> o.getItem().getName());
        }
        return all;
    }

    /**
     * order 안에 orderitem도 dto형태로 받자(절대 entity를 노출시키지 말자!!!)->stream map 사용
     * {
     *         "orderId": 4,
     *         "name": "userA",
     *         "orderdate": "2022-01-08T14:59:38.214003",
     *         "orderStatus": "ORDER",
     *         "address": {
     *             "city": "서울",
     *             "street": "1",
     *             "zipcode": "1111"
     *         },
     *         "orderItems": [
     *             {
     *                 "itemName": "JPA1 Book",
     *                 "orderPrice": 10000,
     *                 "count": 1
     *             },
     *             {
     *                 "itemName": "JPA2 Book",
     *                 "orderPrice": 20000,
     *                 "count": 2
     *             }
     *         ]
     *     }
     *
     */
    @GetMapping("/api/v2/orders")
    public List<OrderDto> ordersV2() {
        List<Order> orders = orderRepository.findAllByString(new OrderSearch());
        List<OrderDto> result = orders.stream()
                .map(o -> new OrderDto(o))
                .collect(Collectors.toList());
        return result;
    }

    /**
     * distinct 사용하여 중복제거
     * 페치 조인으로 sql이 1번만 실행 됨
     * Jpa의 distinct는 SQL에 distinct 를 추가하고, 더해서 같은 에티티가 조회되면
     * 애플리케이션에서 중복을 걸러줌, order 가 컬랙션 페치 조인 때문에 중복조회 되는 것을
     * 막아줌
     *
     * 단점 - *******페이지 불가능******
     * warn 내면서 메모리에서 해주긴하지만 모든 데이터들을 상대로 실험해야됨!!!!메모리 아웃됨;;;
     * 
     */
    @GetMapping("/api/v3/orders")
    public List<OrderDto> orders3() {
        List<Order> orders = orderRepository.findAllWithItem();

        for (Order order : orders) {
            System.out.println("order ref =" + order + " Id = " + order.getId());

        }
        List<OrderDto> result = orders.stream()
                .map(o -> new OrderDto(o))
                .collect(Collectors.toList());
        return result;
    }

    /**
     * no properties라고 뜰경우 있는데 Data가 존재하지 않는 경우
     */
    @Data
    static class OrderDto {

        private Long orderId;
        private String name;
        private LocalDateTime orderdate;
        private OrderStatus orderStatus;
        private Address address;
        private List<OrderItemDto> orderItems;

        public OrderDto(Order order) {
            orderId = order.getId();
            name = order.getMember().getName();
            orderdate = order.getOrderDate();
            orderStatus = order.getStatus();
            address = order.getDelivery().getAddress();
            orderItems = order.getOrderItems().stream()
                    .map(orderItem -> new OrderItemDto(orderItem))
                    .collect(Collectors.toList());
        }
    }

    @Getter
    static class OrderItemDto {

        private String itemName; //상품명
        private int orderPrice; //주문가격
        private int count; // 주문수량


        public OrderItemDto(OrderItem orderItem) {
            itemName = orderItem.getItem().getName();
            orderPrice = orderItem.getOrderPrice();
            count = orderItem.getCount();

        }
    }
}

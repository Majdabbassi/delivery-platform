package com.swiftdeliver.backend.service;

import com.swiftdeliver.backend.entity.Order;
import com.swiftdeliver.backend.repository.CustomerUserRepository;
import com.swiftdeliver.backend.repository.DeliveryCompanyRepository;
import com.swiftdeliver.backend.repository.DeliveryOwnerRepository;
import com.swiftdeliver.backend.repository.DriverPersonRepository;
import com.swiftdeliver.backend.repository.OrderRepository;
import com.swiftdeliver.backend.repository.PartnershipRepository;
import com.swiftdeliver.backend.repository.ProductRepository;
import com.swiftdeliver.backend.repository.VendorCompanyRepository;
import com.swiftdeliver.backend.repository.VendorOwnerRepository;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
@Transactional
public class DashboardService {

    @Resource
    private CustomerUserRepository customerUserRepository;

    @Resource
    private OrderRepository orderRepository;

    @Resource
    private VendorCompanyRepository vendorCompanyRepository;

    @Resource
    private VendorOwnerRepository vendorOwnerRepository;

    @Resource
    private DeliveryCompanyRepository deliveryCompanyRepository;

    @Resource
    private DeliveryOwnerRepository deliveryOwnerRepository;

    @Resource
    private DriverPersonRepository driverPersonRepository;

    @Resource
    private ProductRepository productRepository;

    @Resource
    private PartnershipRepository partnershipRepository;

    @Transactional(readOnly = true)
    public Map<String, Object> getDashboardOverview() {
        Map<String, Object> overview = new LinkedHashMap<>();
        overview.put("customers", customerUserRepository.count());
        overview.put("orders", orderRepository.count());
        overview.put("totalRevenue", orderRepository.getTotalCompletedRevenue());
        overview.put("pendingOrders", orderRepository.countByStatus(Order.OrderStatus.PENDING));
        overview.put("inProgressOrders", orderRepository.countByStatus(Order.OrderStatus.IN_PROGRESS));
        overview.put("completedOrders", orderRepository.countByStatus(Order.OrderStatus.DELIVERED));
        overview.put("cancelledOrders", orderRepository.countByStatus(Order.OrderStatus.CANCELLED));
        overview.put("vendorCompanies", vendorCompanyRepository.count());
        overview.put("vendorOwners", vendorOwnerRepository.count());
        overview.put("deliveryCompanies", deliveryCompanyRepository.count());
        overview.put("deliveryOwners", deliveryOwnerRepository.count());
        overview.put("drivers", driverPersonRepository.count());
        overview.put("products", productRepository.count());
        overview.put("partnerships", partnershipRepository.count());
        return overview;
    }
}
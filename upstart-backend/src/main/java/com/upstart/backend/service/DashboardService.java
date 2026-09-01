package com.upstart.backend.service;

import com.upstart.backend.entity.Order;
import com.upstart.backend.repository.CustomerUserRepository;
import com.upstart.backend.repository.DeliveryCompanyRepository;
import com.upstart.backend.repository.DeliveryOwnerRepository;
import com.upstart.backend.repository.DriverPersonRepository;
import com.upstart.backend.repository.OrderRepository;
import com.upstart.backend.repository.PartnershipRepository;
import com.upstart.backend.repository.ProductRepository;
import com.upstart.backend.repository.VendorCompanyRepository;
import com.upstart.backend.repository.VendorOwnerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
@Transactional
public class DashboardService {

    @Autowired
    private CustomerUserRepository customerUserRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private VendorCompanyRepository vendorCompanyRepository;

    @Autowired
    private VendorOwnerRepository vendorOwnerRepository;

    @Autowired
    private DeliveryCompanyRepository deliveryCompanyRepository;

    @Autowired
    private DeliveryOwnerRepository deliveryOwnerRepository;

    @Autowired
    private DriverPersonRepository driverPersonRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private PartnershipRepository partnershipRepository;

    @Transactional(readOnly = true)
    public Map<String, Object> getDashboardOverview() {
        Map<String, Object> overview = new LinkedHashMap<>();
        overview.put("customers", customerUserRepository.count());
        overview.put("orders", orderRepository.count());
        overview.put("totalRevenue", orderRepository.getTotalCompletedRevenue());
        overview.put("pendingOrders", orderRepository.countByStatus(Order.OrderStatus.PENDING));
        overview.put("inProgressOrders", orderRepository.countByStatus(Order.OrderStatus.IN_PROGRESS));
        overview.put("completedOrders", orderRepository.countByStatus(Order.OrderStatus.COMPLETED));
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
DROP DATABASE IF EXISTS glowbook;
CREATE DATABASE glowbook CHARACTER SET utf8mb4 COLLATE utf8mb4_turkish_ci;

USE glowbook;

-- ===========================
-- CUSTOMERS (ÜYE MÜŞTERİLER)
-- ===========================

CREATE TABLE customers (
    customer_id INT AUTO_INCREMENT PRIMARY KEY,
    first_name VARCHAR(50) NOT NULL,
    last_name VARCHAR(50) NOT NULL,
    phone VARCHAR(15) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ===========================
-- EMPLOYEES
-- ===========================

CREATE TABLE employees (
    employee_id VARCHAR(10) PRIMARY KEY,

    first_name VARCHAR(50) NOT NULL,

    last_name VARCHAR(50) NOT NULL,

    password VARCHAR(255) NOT NULL,

    phone VARCHAR(15),

    email VARCHAR(100) UNIQUE,

    is_active BOOLEAN DEFAULT TRUE
);

-- ===========================
-- SERVICES
-- ===========================

CREATE TABLE services (

    service_id INT AUTO_INCREMENT PRIMARY KEY,

    service_name VARCHAR(100) NOT NULL,

    description TEXT,

    service_image VARCHAR(255),

    is_active BOOLEAN DEFAULT TRUE

);

-- ===========================
-- SERVICE OPTIONS
-- (Örneğin 1 Bölge, 3 Bölge...)
-- ===========================

CREATE TABLE service_options (

    option_id INT AUTO_INCREMENT PRIMARY KEY,

    service_id INT NOT NULL,

    option_name VARCHAR(100) NOT NULL,

    price DECIMAL(10,2) NOT NULL,

    is_active BOOLEAN DEFAULT TRUE,

    FOREIGN KEY(service_id)
        REFERENCES services(service_id)
        ON DELETE CASCADE
);

-- ===========================
-- SERVICE PACKAGES
-- ===========================

CREATE TABLE service_packages (

    package_id INT AUTO_INCREMENT PRIMARY KEY,

    service_id INT NOT NULL,

    package_name VARCHAR(100) NOT NULL,

    description TEXT,

    total_session INT NOT NULL,

    price DECIMAL(10,2) NOT NULL,

    package_image VARCHAR(255),

    is_active BOOLEAN DEFAULT TRUE,

    FOREIGN KEY (service_id)
        REFERENCES services(service_id)
        ON DELETE CASCADE
);
-- ===========================
-- CUSTOMER PACKAGES
-- ===========================

CREATE TABLE customer_packages (

    customer_package_id INT AUTO_INCREMENT PRIMARY KEY,

    customer_id INT NOT NULL,

    package_id INT NOT NULL,

    remaining_session INT NOT NULL,

    purchase_price DECIMAL(10,2) NOT NULL,

    purchase_date DATE NOT NULL,

    is_active BOOLEAN DEFAULT TRUE,

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    FOREIGN KEY(customer_id)
        REFERENCES customers(customer_id)
        ON DELETE CASCADE,

    FOREIGN KEY(package_id)
        REFERENCES service_packages(package_id)
        ON DELETE RESTRICT

);

CREATE TABLE employee_services (

    employee_service_id INT AUTO_INCREMENT PRIMARY KEY,

    employee_id VARCHAR(10) NOT NULL,

    service_id INT NOT NULL,

    is_active BOOLEAN DEFAULT TRUE,

    FOREIGN KEY (employee_id)
        REFERENCES employees(employee_id)
        ON DELETE CASCADE,

    FOREIGN KEY (service_id)
        REFERENCES services(service_id)
        ON DELETE CASCADE
);

CREATE TABLE working_hours (

    working_hour_id INT AUTO_INCREMENT PRIMARY KEY,

    day_of_week ENUM(
        'MONDAY',
        'TUESDAY',
        'WEDNESDAY',
        'THURSDAY',
        'FRIDAY',
        'SATURDAY',
        'SUNDAY'
    ) NOT NULL,

    start_time TIME NOT NULL,

    end_time TIME NOT NULL,

    is_closed BOOLEAN DEFAULT FALSE
);

CREATE TABLE employee_leaves (

    leave_id INT AUTO_INCREMENT PRIMARY KEY,

    employee_id VARCHAR(10) NOT NULL,

    leave_date DATE NOT NULL,

    reason VARCHAR(255),

    FOREIGN KEY (employee_id)
        REFERENCES employees(employee_id)
        ON DELETE CASCADE
);

CREATE TABLE holidays (

    holiday_id INT AUTO_INCREMENT PRIMARY KEY,

    holiday_date DATE NOT NULL UNIQUE,

    holiday_name VARCHAR(100) NOT NULL,

    description VARCHAR(255)
);

CREATE TABLE appointments (

    appointment_id INT AUTO_INCREMENT PRIMARY KEY,

    customer_id INT NULL,

    customer_package_id INT NULL,

    customer_name VARCHAR(50) NOT NULL,

    customer_surname VARCHAR(50) NOT NULL,

    phone VARCHAR(15) NOT NULL,

    employee_id VARCHAR(10) NOT NULL,

    service_id INT NOT NULL,

    option_id INT NOT NULL,

    appointment_date DATE NOT NULL,

    appointment_time TIME NOT NULL,

    price DECIMAL(10,2) NOT NULL,

    status ENUM(
        'PENDING',
        'APPROVED',
        'COMPLETED',
        'CANCELLED'
    ) DEFAULT 'PENDING',

    cancellation_reason VARCHAR(255),

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    FOREIGN KEY (customer_id)
        REFERENCES customers(customer_id)
        ON DELETE SET NULL,

    FOREIGN KEY (customer_package_id)
        REFERENCES customer_packages(customer_package_id)
        ON DELETE SET NULL,

    FOREIGN KEY (employee_id)
        REFERENCES employees(employee_id),

    FOREIGN KEY (service_id)
        REFERENCES services(service_id),

    FOREIGN KEY (option_id)
        REFERENCES service_options(option_id)
);

CREATE TABLE notifications (

    notification_id INT AUTO_INCREMENT PRIMARY KEY,

    customer_id INT NULL,

    appointment_id INT NULL,

    type ENUM(
        'APPOINTMENT_CREATED',
        'APPOINTMENT_APPROVED',
        'APPOINTMENT_CANCELLED',
        'APPOINTMENT_REMINDER',
        'WAITING_LIST_MATCH'
    ) NOT NULL,

    title VARCHAR(255) NOT NULL,

    message VARCHAR(500) NOT NULL,

    is_read BOOLEAN DEFAULT FALSE,

    sms_sent BOOLEAN DEFAULT FALSE,

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (customer_id)
        REFERENCES customers(customer_id)
        ON DELETE SET NULL,

    FOREIGN KEY (appointment_id)
        REFERENCES appointments(appointment_id)
        ON DELETE SET NULL
);

CREATE TABLE waiting_list (

    waiting_list_id INT AUTO_INCREMENT PRIMARY KEY,

    customer_id INT NULL,

    service_id INT NOT NULL,

    option_id INT NOT NULL,

    customer_name VARCHAR(50) NOT NULL,

    customer_surname VARCHAR(50) NOT NULL,

    phone VARCHAR(15) NOT NULL,

    preferred_date DATE NOT NULL,

    preferred_start_time TIME NULL,

    preferred_end_time TIME NULL,

    status ENUM(
        'ACTIVE',
        'NOTIFIED',
        'CONVERTED',
        'CANCELLED'
    ) DEFAULT 'ACTIVE',

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    FOREIGN KEY (customer_id)
        REFERENCES customers(customer_id)
        ON DELETE SET NULL,

    FOREIGN KEY (service_id)
        REFERENCES services(service_id),

    FOREIGN KEY (option_id)
        REFERENCES service_options(option_id)
);

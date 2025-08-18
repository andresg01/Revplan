INSERT INTO BRANDS (id, name)
VALUES
(1, 'ZARA'),
(2, 'ZARA 2');

INSERT INTO PRICES (id, brand_id, product_id, price_list, start_date, end_date, priority, price, curr)
VALUES
(nextval('sq_prices'), 1, 100, 1, '2020-06-14 00:00:00', '2020-12-31 23:59:59', 0, 35.50, 'EUR'),
(nextval('sq_prices'), 1, 100, 2, '2020-06-14 00:00:00', '2020-06-14 23:59:59', 1, 25.45, 'EUR'),
(nextval('sq_prices'), 1, 100, 3, '2020-06-14 00:00:00', '2020-07-31 23:59:59', 0, 35.50, 'EUR'),
(nextval('sq_prices'), 2, 102, 1, '2020-06-14 00:00:00', '2020-12-31 23:59:59', 1, 30.50, 'EUR'),
(nextval('sq_prices'), 2, 102, 2, '2020-06-14 00:00:00', '2020-12-31 23:59:59', 1, 38.95, 'EUR'),
(nextval('sq_prices'), 2, 102, 3, '2020-06-14 00:00:00', '2020-12-31 23:59:59', 1, 38.95, 'EUR');
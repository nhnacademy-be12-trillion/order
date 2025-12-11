-- 배송 정책
INSERT INTO "delivery_policy" ("deliverypolicy_fee", "deliverypolicy_threshold") VALUES (3000, 50000);

-- 포장 정책
INSERT INTO "packaging" ("packaging_type", "packaging_price") VALUES ('WOOD', 500);
INSERT INTO "packaging" ("packaging_type", "packaging_price") VALUES ('STONE', 1000);
INSERT INTO "packaging" ("packaging_type", "packaging_price") VALUES ('IRON', 2000);

-- 샘플 주문
INSERT INTO "order" ("order_number", "member_id", "non_member_password", "order_status", 
                     "orderer_name", "orderer_contact", 
                     "receiver_name", "receiver_contact", "receiver_address",
                     "order_date", "shipping_post_code", "delivery_date", "delivery_fee", "point_usage",
                     "origin_price", "total_price", "coupon_id") 
VALUES ('ORD-SAMPLE-008', 1, null, 'PENDING',
        '테스트 주문자', '010-1234-5678',
        '테스트 수령인', '010-9876-5432', '광주광역시 동구 제봉로 92',
        CURRENT_TIMESTAMP, '61452', CURRENT_TIMESTAMP + INTERVAL '3' DAY, 3000, 2000,
        55000, 56000, 101);

INSERT INTO "order" ("order_number", "member_id", "non_member_password", "order_status",
                     "orderer_name", "orderer_contact",
                     "receiver_name", "receiver_contact", "receiver_address",
                     "order_date", "shipping_post_code", "delivery_date", "delivery_fee", "point_usage",
                     "origin_price", "total_price", "coupon_id")
VALUES ('ORD-SAMPLE-002', null, '$2a$12$CHTyAGNLcQI9JdQHplZoCucsC1IgBdfnJPGg4R9WZ01sYOPrWYmR2', 'PENDING',
        '테스트 주문자', '010-1234-5678',
        '테스트 수령인', '010-9876-5432', '광주광역시 동구 제봉로 92',
        CURRENT_TIMESTAMP, '61452', CURRENT_TIMESTAMP + INTERVAL '3' DAY, 3000, 2000,
        55000, 56000, 101);

-- 샘플 주문 상품
INSERT INTO "order_item" ("order_id", "book_id", "quantity", "price", "shipping_date", "packaging_price", "orderitem_status")
VALUES (1, 1, 1, 30000, null, 500, 'PREPARING');

INSERT INTO "order_item" ("order_id", "book_id", "quantity", "price", "shipping_date", "packaging_price", "orderitem_status")
VALUES (1, 2, 1, 25000, null, 0, 'PREPARING');

INSERT INTO "order_item" ("order_id", "book_id", "quantity", "price", "shipping_date", "packaging_price", "orderitem_status")
VALUES (2, 1, 1, 30000, null, 500, 'PREPARING');

INSERT INTO "order_item" ("order_id", "book_id", "quantity", "price", "shipping_date", "packaging_price", "orderitem_status")
VALUES (2, 2, 1, 25000, null, 0, 'PREPARING');
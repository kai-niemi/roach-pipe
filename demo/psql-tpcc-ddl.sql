CREATE TABLE public.warehouse
(
    w_id       INT8           NOT NULL,
    w_name     VARCHAR(10)    NULL,
    w_street_1 VARCHAR(20)    NULL,
    w_street_2 VARCHAR(20)    NULL,
    w_city     VARCHAR(20)    NULL,
    w_state    CHAR(2)        NULL,
    w_zip      CHAR(9)        NULL,
    w_tax      DECIMAL(4, 4)  NULL,
    w_ytd      DECIMAL(12, 2) NULL,

    PRIMARY KEY (w_id)
);

CREATE TABLE public.district
(
    d_id        INT8           NOT NULL,
    d_w_id      INT8           NOT NULL,
    d_name      VARCHAR(10)    NULL,
    d_street_1  VARCHAR(20)    NULL,
    d_street_2  VARCHAR(20)    NULL,
    d_city      VARCHAR(20)    NULL,
    d_state     CHAR(2)        NULL,
    d_zip       CHAR(9)        NULL,
    d_tax       DECIMAL(4, 4)  NULL,
    d_ytd       DECIMAL(12, 2) NULL,
    d_next_o_id INT8           NULL,

    PRIMARY KEY (d_w_id, d_id)
);

CREATE TABLE public.customer
(
    c_id           INT8           NOT NULL,
    c_d_id         INT8           NOT NULL,
    c_w_id         INT8           NOT NULL,
    c_first        VARCHAR(16)    NULL,
    c_middle       CHAR(2)        NULL,
    c_last         VARCHAR(16)    NULL,
    c_street_1     VARCHAR(20)    NULL,
    c_street_2     VARCHAR(20)    NULL,
    c_city         VARCHAR(20)    NULL,
    c_state        CHAR(2)        NULL,
    c_zip          CHAR(9)        NULL,
    c_phone        CHAR(16)       NULL,
    c_since        TIMESTAMP      NULL,
    c_credit       CHAR(2)        NULL,
    c_credit_lim   DECIMAL(12, 2) NULL,
    c_discount     DECIMAL(4, 4)  NULL,
    c_balance      DECIMAL(12, 2) NULL,
    c_ytd_payment  DECIMAL(12, 2) NULL,
    c_payment_cnt  INT8           NULL,
    c_delivery_cnt INT8           NULL,
    c_data         VARCHAR(500)   NULL,

    PRIMARY KEY (c_w_id, c_d_id, c_id)
);

CREATE INDEX customer_idx ON customer (c_w_id, c_d_id, c_last, c_first);

CREATE TABLE public."order"
(
    o_id         INT8      NOT NULL,
    o_d_id       INT8      NOT NULL,
    o_w_id       INT8      NOT NULL,
    o_c_id       INT8      NULL,
    o_entry_d    TIMESTAMP NULL,
    o_carrier_id INT8      NULL,
    o_ol_cnt     INT8      NULL,
    o_all_local  INT8      NULL,

    PRIMARY KEY (o_w_id, o_d_id, o_id)
);

CREATE UNIQUE INDEX order_idx ON "order" (o_w_id ASC, o_d_id ASC, o_c_id ASC, o_id DESC);

-- CREATE extension pgcrypto;

CREATE TABLE public.history
(
    rowid    UUID          NOT NULL DEFAULT gen_random_uuid(),
    h_c_id   INT8          NOT NULL,
    h_c_d_id INT8          NOT NULL,
    h_c_w_id INT8          NOT NULL,
    h_d_id   INT8          NOT NULL,
    h_w_id   INT8          NOT NULL,
    h_date   TIMESTAMP     NULL,
    h_amount DECIMAL(6, 2) NULL,
    h_data   VARCHAR(24)   NULL,

    PRIMARY KEY (h_w_id, rowid)
);

CREATE INDEX history_customer_fk_idx ON history (h_c_w_id ASC, h_c_d_id ASC, h_c_id ASC);
CREATE INDEX history_district_fk_idx ON history (h_w_id ASC, h_d_id ASC);

CREATE TABLE public.item
(
    i_id    INT8          NOT NULL,
    i_im_id INT8          NULL,
    i_name  VARCHAR(24)   NULL,
    i_price DECIMAL(5, 2) NULL,
    i_data  VARCHAR(50)   NULL,

    PRIMARY KEY (i_id)
);

CREATE TABLE public.new_order
(
    no_o_id INT8 NOT NULL,
    no_d_id INT8 NOT NULL,
    no_w_id INT8 NOT NULL,

    PRIMARY KEY (no_w_id, no_d_id, no_o_id)
);

CREATE TABLE public.stock
(
    s_i_id       INT8        NOT NULL,
    s_w_id       INT8        NOT NULL,
    s_quantity   INT8        NULL,
    s_dist_01    CHAR(24)    NULL,
    s_dist_02    CHAR(24)    NULL,
    s_dist_03    CHAR(24)    NULL,
    s_dist_04    CHAR(24)    NULL,
    s_dist_05    CHAR(24)    NULL,
    s_dist_06    CHAR(24)    NULL,
    s_dist_07    CHAR(24)    NULL,
    s_dist_08    CHAR(24)    NULL,
    s_dist_09    CHAR(24)    NULL,
    s_dist_10    CHAR(24)    NULL,
    s_ytd        INT8        NULL,
    s_order_cnt  INT8        NULL,
    s_remote_cnt INT8        NULL,
    s_data       VARCHAR(50) NULL,

    PRIMARY KEY (s_w_id, s_i_id)
);

CREATE INDEX stock_item_fk_idx ON stock (s_i_id ASC);

CREATE TABLE public.order_line
(
    ol_o_id        INT8          NOT NULL,
    ol_d_id        INT8          NOT NULL,
    ol_w_id        INT8          NOT NULL,
    ol_number      INT8          NOT NULL,
    ol_i_id        INT8          NOT NULL,
    ol_supply_w_id INT8          NULL,
    ol_delivery_d  TIMESTAMP     NULL,
    ol_quantity    INT8          NULL,
    ol_amount      DECIMAL(6, 2) NULL,
    ol_dist_info   CHAR(24)      NULL,

    PRIMARY KEY (ol_w_id, ol_d_id, ol_o_id, ol_number)
);

CREATE INDEX order_line_stock_fk_idx ON order_line (ol_supply_w_id ASC, ol_i_id ASC);

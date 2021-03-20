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
    CONSTRAINT "primary" PRIMARY KEY (w_id ASC),
    FAMILY     "primary"(w_id, w_name, w_street_1, w_street_2, w_city, w_state, w_zip, w_tax, w_ytd)
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
    CONSTRAINT "primary" PRIMARY KEY (d_w_id ASC, d_id ASC),
    FAMILY      "primary"(d_id, d_w_id, d_name, d_street_1, d_street_2, d_city, d_state, d_zip, d_tax, d_ytd, d_next_o_id)
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
    CONSTRAINT "primary" PRIMARY KEY (c_w_id ASC, c_d_id ASC, c_id ASC),
    INDEX          customer_idx(c_w_id ASC, c_d_id ASC, c_last ASC, c_first ASC),
    FAMILY         "primary"(c_id, c_d_id, c_w_id, c_first, c_middle, c_last, c_street_1, c_street_2, c_city, c_state, c_zip, c_phone, c_since, c_credit, c_credit_lim, c_discount, c_balance, c_ytd_payment, c_payment_cnt, c_delivery_cnt, c_data)
);

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
    CONSTRAINT "primary" PRIMARY KEY (o_w_id ASC, o_d_id ASC, o_id DESC),
    UNIQUE INDEX order_idx (o_w_id ASC, o_d_id ASC, o_c_id ASC, o_id DESC) STORING (o_entry_d, o_carrier_id),
    FAMILY       "primary"(o_id, o_d_id, o_w_id, o_c_id, o_entry_d, o_carrier_id, o_ol_cnt, o_all_local)
);

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
    CONSTRAINT "primary" PRIMARY KEY (h_w_id ASC, rowid ASC),
    INDEX    history_customer_fk_idx(h_c_w_id ASC, h_c_d_id ASC, h_c_id ASC),
    INDEX    history_district_fk_idx(h_w_id ASC, h_d_id ASC),
    FAMILY   "primary"(rowid, h_c_id, h_c_d_id, h_c_w_id, h_d_id, h_w_id, h_date, h_amount, h_data)
);

CREATE TABLE public.item
(
    i_id    INT8          NOT NULL,
    i_im_id INT8          NULL,
    i_name  VARCHAR(24)   NULL,
    i_price DECIMAL(5, 2) NULL,
    i_data  VARCHAR(50)   NULL,
    CONSTRAINT "primary" PRIMARY KEY (i_id ASC),
    FAMILY  "primary"(i_id, i_im_id, i_name, i_price, i_data)
);

CREATE TABLE public.new_order
(
    no_o_id INT8 NOT NULL,
    no_d_id INT8 NOT NULL,
    no_w_id INT8 NOT NULL,
    CONSTRAINT "primary" PRIMARY KEY (no_w_id ASC, no_d_id ASC, no_o_id ASC),
    FAMILY  "primary"(no_o_id, no_d_id, no_w_id)
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
    CONSTRAINT "primary" PRIMARY KEY (s_w_id ASC, s_i_id ASC),
    INDEX        stock_item_fk_idx(s_i_id ASC),
    FAMILY       "primary"(s_i_id, s_w_id, s_quantity, s_dist_01, s_dist_02, s_dist_03, s_dist_04, s_dist_05, s_dist_06, s_dist_07, s_dist_08, s_dist_09, s_dist_10, s_ytd, s_order_cnt, s_remote_cnt, s_data)
);

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
    CONSTRAINT "primary" PRIMARY KEY (ol_w_id ASC, ol_d_id ASC, ol_o_id DESC, ol_number ASC),
    INDEX          order_line_stock_fk_idx(ol_supply_w_id ASC, ol_i_id ASC),
    FAMILY         "primary"(ol_o_id, ol_d_id, ol_w_id, ol_number, ol_i_id, ol_supply_w_id, ol_delivery_d, ol_quantity, ol_amount, ol_dist_info)
);

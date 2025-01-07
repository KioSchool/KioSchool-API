update order_product op
set product_name = p.name,
    product_price = p.price
    from product p
where op.product_id = p.id
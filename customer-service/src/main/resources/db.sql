create table if not exists customers
(
    id              serial primary key,
    name            varchar not null unique,
    update_datetime timestamp default now()
);



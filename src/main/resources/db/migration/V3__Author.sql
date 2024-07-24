create table author
(
    id         serial primary key,
    full_name  text,
    created_at timestamp default current_timestamp
);

alter table budget
    add column author_id int references author;
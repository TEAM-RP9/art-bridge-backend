delete from artwork;

alter table artwork drop column image_url;
alter table artwork alter column description drop not null;
alter table artwork alter column medium drop not null;
alter table artwork alter column style drop not null;
alter table artwork alter column creation_year drop not null;

alter table artwork add column category varchar(40);
alter table artwork add column width numeric(10,2);
alter table artwork add column height numeric(10,2);
alter table artwork add column dimension_unit varchar(20);
alter table artwork add column tags text[] not null default '{}';
alter table artwork add column status varchar(16) not null default 'DRAFT';
alter table artwork add column show_on_profile boolean not null default false;
alter table artwork add column media_id bigint not null references media(id);
alter table artwork add constraint artwork_media_id_unique unique (media_id);

drop index if exists idx_artworks_filter;
create index idx_artworks_filter on artwork (category, medium, creation_year);
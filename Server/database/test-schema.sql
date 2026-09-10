drop database if exists golf_trip_booker_test;
create database golf_trip_booker_test;
use golf_trip_booker_test;

create table app_user (
    user_id  int primary key auto_increment,
    email    varchar(100) not null unique,
    username varchar(50)  not null unique,
    password varchar(100) not null,
    role     enum('CLIENT', 'HOST') not null default 'CLIENT'
);

create table destination (
    destination_id int primary key auto_increment,
    name           varchar(100) not null,
    region         varchar(100),
    description    text,
    latitude       decimal(9, 6),
    longitude      decimal(9, 6)
);

create table trip_request (
    request_id        int primary key auto_increment,
    user_id           int not null,
    -- Nullable: a client may ask for somewhere that is not seeded yet, naming it in
    -- requested_place instead. Exactly one of the two is always set -- TripRequestService
    -- enforces that, since a CHECK constraint would not tell the user what went wrong.
    destination_id    int,
    requested_place     varchar(150),
    requested_latitude  decimal(9, 6),
    requested_longitude decimal(9, 6),
    budget_per_player decimal(10, 2) not null,
    player_count      int not null,
    rounds_requested  int not null,
    nights            int not null,
    -- The window the group can travel in. Both or neither, and latest >= earliest --
    -- TripRequestService enforces that pair the same way it does a coordinate pair,
    -- since a CHECK constraint could not say which half was missing.
    -- A group with one fixed date sets both to the same day.
    earliest_start    date,
    latest_start      date,
    notes             text,
    status            enum('PENDING', 'PROPOSED', 'BOOKED', 'DECLINED', 'CANCELLED') not null default 'PENDING',
    decline_reason    varchar(255),
    created_at        datetime not null,
    constraint fk_trip_request_app_user
        foreign key (user_id) references app_user (user_id),
    constraint fk_trip_request_destination
        foreign key (destination_id) references destination (destination_id)
);

create table booking (
    booking_id  int primary key auto_increment,
    request_id  int not null,
    courses     text not null,
    lodging     varchar(150),
    start_date  date not null,
    end_date    date not null,
    total_cost  decimal(10, 2) not null,
    itinerary   text,
    booked_by   int not null,
    booked_at   datetime not null,
    -- Null until the client accepts. Every host submission starts life as a proposal,
    -- so this is the one column separating "the host arranged it" from "we agreed".
    confirmed_at datetime,
    /*
     * How a proposal died. countered_at is the CLIENT turning it down, with a reason and
     * a note; withdrawn_at is the HOST pulling it back before anyone answered, with a
     * note of their own. Both stay in the table as history.
     *
     * Both notes are required, and that symmetry is deliberate. A first pass demanded an
     * explanation from the client and let the host yank a proposal in silence -- which is
     * the same failure the counter flow exists to prevent, pointed the other way, and
     * worse, because the client did not start it and would see their trip revert to
     * pending with nothing said.
     *
     * Note this is not trip_request.decline_reason, which is the host declining the whole
     * request. Different author, different verb, so deliberately a different column.
     */
    countered_at   datetime,
    counter_reason enum('PRICE', 'DATES', 'COURSES', 'LODGING', 'OTHER'),
    counter_note   varchar(500),
    withdrawn_at   datetime,
    withdraw_note  varchar(500),
    /*
     * A request may collect several proposals over time but only ever one confirmed
     * booking. A unique index holds any number of NULLs, so a flag that is NULL on an
     * unconfirmed row allows repeat proposals while still refusing a second confirmed one.
     * unique (request_id, confirmed_at) would not do it -- two confirmed rows carry
     * different timestamps, so both would be allowed.
     */
    is_confirmed tinyint generated always as (if(confirmed_at is null, null, 1)) stored,
    constraint uq_booking_one_confirmed unique (request_id, is_confirmed),
    constraint fk_booking_trip_request
        foreign key (request_id) references trip_request (request_id),
    constraint fk_booking_app_user
        foreign key (booked_by) references app_user (user_id)
);

-- Every test method calls this first, so each test starts from the same rows.
-- TestDataHelper.java mirrors it. Change one, change the other in the same commit.
delimiter //
create procedure set_known_good_state()
begin
    delete from booking;
    delete from trip_request;
    delete from destination;
    delete from app_user;

    alter table app_user     auto_increment = 1;
    alter table destination  auto_increment = 1;
    alter table trip_request auto_increment = 1;
    alter table booking      auto_increment = 1;

    insert into app_user (email, username, password, role) values
        ("a@a.com", "clientA", "passwordA", "CLIENT"),
        ("b@b.com", "clientB", "passwordB", "CLIENT"),
        ("h@h.com", "hostH",   "passwordH", "HOST");

    insert into destination (name, region, description, latitude, longitude) values
        ("Pinehurst",    "North Carolina", "Nine courses in one village.",
         35.195400, -79.469500),
        ("Bandon Dunes", "Oregon",         "Walking-only links on the Pacific.",
         43.189000, -124.392000),
        ("Streamsong",   "Florida",        "Three courses built on a phosphate mine.",
         27.662000, -81.899000);

    insert into trip_request
        (user_id, destination_id, requested_place, requested_latitude, requested_longitude,
         budget_per_player, player_count, rounds_requested,
         nights, earliest_start, latest_start, notes, status, decline_reason, created_at) values
        (1, 1, null, null, null, 1200.00, 4, 3, 4, "2026-10-12", "2026-10-12",
         "Two rounds a day if the budget allows", "BOOKED", null, "2026-08-02 09:00:00"),
        (1, 2, null, null, null, 2000.00, 2, 3, 3, "2026-09-03", "2026-09-17", null,
         "PENDING", null, "2026-08-09 10:00:00"),
        (2, 3, null, null, null,  900.00, 8, 2, 2, null, null, null,
         "PENDING", null, "2026-08-12 11:00:00"),
        -- Somewhere not seeded yet. Exercises the left join, and the promotion a
        -- host booking would trigger.
        (2, null, "Missoula, Montana", 46.872200, -113.994000, 700.00, 4, 2, 2,
         "2026-10-05", "2026-10-05", null, "PENDING", null, "2026-08-15 12:00:00");

    insert into booking
        (request_id, courses, lodging, start_date, end_date, total_cost, itinerary, booked_by,
         booked_at, confirmed_at) values
        (1, "Pinehurst No. 2\nPinehurst No. 4\nMid Pines", "The Carolina Hotel",
         "2026-10-12", "2026-10-16", 4650.00, "Arrive Monday, first tee Tuesday.", 3,
         "2026-08-03 12:00:00", "2026-08-03 12:00:00");
end //
delimiter ;

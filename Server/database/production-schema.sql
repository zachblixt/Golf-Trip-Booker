drop database if exists golf_trip_booker;
create database golf_trip_booker;
use golf_trip_booker;

-- Appears twice below: as the client who submits, and as the host who books.
create table app_user (
    user_id  int primary key auto_increment,
    email    varchar(100) not null unique,
    username varchar(50)  not null unique,
    -- BCrypt hash. Never returned by the API.
    password varchar(100) not null,
    -- Registration always creates a CLIENT. Hosts are seeded by hand.
    role     enum('CLIENT', 'HOST') not null default 'CLIENT'
);

-- The area a client picks. Seeded, never free text.
-- decimal(9,6) is the standard for coordinates: six decimal places is about
-- 10cm of precision, and 9 total digits leaves room for -179.999999.
create table destination (
    destination_id int primary key auto_increment,
    name           varchar(100) not null,
    region         varchar(100),
    description    text,
    latitude       decimal(9, 6),
    longitude      decimal(9, 6)
);

-- What the client asked for. Only PENDING can transition.
create table trip_request (
    request_id        int primary key auto_increment,
    -- The client. Taken from the JWT, never from the request body.
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
    -- Set only when DECLINED.
    decline_reason    varchar(255),
    created_at        datetime not null,
    constraint fk_trip_request_app_user
        foreign key (user_id) references app_user (user_id),
    constraint fk_trip_request_destination
        foreign key (destination_id) references destination (destination_id)
);

-- What the host arranged. Created by the host, not the client.
create table booking (
    booking_id  int primary key auto_increment,
    request_id  int not null,
    -- Course names, one per line. Normalized into real rows post-MVP.
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
    -- The ERD had this arrow pointing the other way, which MySQL will not
    -- accept: the child row is the booking, so the FK lives here.
    constraint fk_booking_trip_request
        foreign key (request_id) references trip_request (request_id),
    constraint fk_booking_app_user
        foreign key (booked_by) references app_user (user_id)
);

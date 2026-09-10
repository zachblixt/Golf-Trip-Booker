-- Why is Explore empty?
--
-- Run this against the PRODUCTION database (not the test one) and read the four
-- results top to bottom. The first one that looks wrong is the answer.
--
--   mysql -u root -p < Server/database/diagnose-explore.sql

use golf_trip_booker;

-- 1. Does trip_request have the three new columns?
--
-- This is the likeliest cause. `mvn test` rebuilds nothing but the TEST database,
-- so all 116 tests pass against golf_trip_booker_test while the production schema
-- is still the old one. Every trip_request query now selects tr.requested_place,
-- so against an old table they all fail -- Explore, My Trips, the host queue, the
-- lot -- and the app shows nothing.
--
-- Expect 3. Anything less means production-schema.sql was not re-run, and the fix
-- is to run production-schema.sql then initial-data.sql.
select count(*) as new_columns_present_expect_3
from information_schema.columns
where table_schema = 'golf_trip_booker'
  and table_name = 'trip_request'
  and column_name in ('requested_place', 'requested_latitude', 'requested_longitude');

-- 2. Is the seed actually in there?
--
-- Expect 40 / 59 / 50. Zeros mean initial-data.sql never ran, or ran against a
-- different database, or failed partway -- check the console for the first error
-- rather than the last.
select (select count(*) from destination)  as destinations_expect_40,
       (select count(*) from trip_request) as requests_expect_59,
       (select count(*) from booking)      as bookings_expect_50;

-- 3. Do the bookings line up with BOOKED requests?
--
-- This is what findBooked joins on. Expect 50 and 0. A non-zero orphan count means
-- the two inserts drifted apart, and Explore is correctly showing nothing.
select (select count(*)
        from trip_request tr
        inner join booking b on b.request_id = tr.request_id
        where tr.status = 'BOOKED'
          and tr.destination_id is not null) as explore_rows_expect_50,
       (select count(*)
        from booking b
        left join trip_request tr on tr.request_id = b.request_id
        where tr.request_id is null
           or tr.status <> 'BOOKED')         as orphaned_bookings_expect_0;

-- 4. The exact query Explore runs, unfiltered.
--
-- If 1-3 all look right and this still returns nothing, the problem is on the
-- client and not here.
select tr.request_id, d.name, tr.player_count,
       round(b.total_cost / tr.player_count, 2) as per_player, b.start_date
from trip_request tr
inner join app_user c on tr.user_id = c.user_id
left join destination d on tr.destination_id = d.destination_id
inner join booking b on b.request_id = tr.request_id
where tr.status = 'BOOKED' and d.destination_id is not null
order by b.start_date desc
limit 10;

-- Why is "Ask for changes" not working?
--
-- Run against the PRODUCTION database and read the four results top to bottom. The
-- first one that looks wrong is the answer.
--
--   mysql -u root -p < Server/database/diagnose-proposals.sql

use golf_trip_booker;

-- 1. Does `booking` have all four columns the code now reads?
--
-- THE LIKELIEST CAUSE. BookingJdbcClientRepository selects countered_at,
-- counter_reason, counter_note, withdrawn_at and withdraw_note in every query. If the
-- schema was last run before those landed, every read of a booking row fails with
-- "Unknown column" and nothing involving a proposal works at all.
--
-- Expect five rows. Fewer means re-run production-schema.sql and initial-data.sql.
select column_name, column_type
from information_schema.columns
where table_schema = 'golf_trip_booker'
  and table_name = 'booking'
  and column_name in ('countered_at', 'counter_reason', 'counter_note',
                      'withdrawn_at', 'withdraw_note')
order by ordinal_position;

-- 2. Does the status enum know the word PROPOSED?
--
-- If this still says OFFERED, the schema is older than the whole rename and the app
-- cannot write the status a counter moves away from.
select column_type as status_enum
from information_schema.columns
where table_schema = 'golf_trip_booker'
  and table_name = 'trip_request'
  and column_name = 'status';

-- 3. Is there actually a live proposal to answer?
--
-- counterProposal calls findLiveProposal first, which wants a row that is neither
-- confirmed, countered, nor withdrawn. No such row means "There is no live proposal
-- on request 51 to answer" -- which is the service working correctly on bad data.
select b.booking_id,
       b.booked_at,
       b.confirmed_at,
       b.countered_at,
       b.withdrawn_at,
       case
           when b.confirmed_at is not null then 'confirmed, not answerable'
           when b.countered_at is not null then 'already countered, history'
           when b.withdrawn_at is not null then 'withdrawn, history'
           else 'LIVE -- this is the one a counter should hit'
       end as state
from booking b
where b.request_id = 51
order by b.booked_at desc, b.booking_id desc;

-- 4. And is the request itself still PROPOSED?
--
-- The conditional update only fires on PROPOSED -> PENDING. Any other status and the
-- counter is refused with "That proposal was already answered."
select request_id, user_id, status, decline_reason
from trip_request
where request_id = 51;

use golf_trip_booker;

/*
 * Seed data for the pitch: a weekend, a few rounds, your buddies, and a number you
 * can say out loud. Fifty trips already played, priced per player from $110 to $955,
 * across forty destinations where those numbers are actually achievable.
 *
 * Deliberately NOT a list of famous resorts. The point of Explore is that somebody
 * opens it and thinks "we could do that", not "must be nice". Where a marquee course
 * appears, it appears at its real rate -- which is why you will find Pinehurst No. 3
 * here and not No. 2.
 *
 * Prices: seven trips are checked against published rates (Thousand Hills and Pointe
 * Royale in Branson, Myrtle Beach packages, the RTJ Trail). The rest are estimates
 * calibrated to those anchors and reviewed for market alignment. All are plausible
 * shoulder-season weekday numbers, not quotes.
 */

-- Every seed account has the password: 1234
--
-- Deliberately short, so signing in during a demo is not four seconds of thumbing a
-- symbol on a phone keyboard. Worth knowing that UserService would refuse it at
-- registration -- a real sign-up needs eight characters with a digit, a letter and a
-- symbol -- and these rows go in underneath that check rather than through it.
--
-- `tourpro` is the only HOST. There is no way to become one through the API.
insert into app_user (email, username, password, role) values
    ("zach@example.com", "zach", "{bcrypt}$2b$10$U8rTl8EHSE7h7y9qR46PZ.GzBmKlFICfbxb9WtHN4Tb1Tw0Itq9fu", "CLIENT"),
    ("dana@example.com", "dana", "{bcrypt}$2b$10$Nld5ckRRjDcwmmS7jKuGn.uWM0OgrIPRPlIzVrnSmL7Edy2h.BZP6", "CLIENT"),
    ("mo@example.com", "mo", "{bcrypt}$2b$10$bVuD/CDpa0XhuXCrC8J3rO6Dz6p//qW4143vV5i96FddFdupi32l6", "CLIENT"),
    ("kyle@example.com", "kyle", "{bcrypt}$2b$10$ndHKvJ5H3CdhaTORrhYs3uVnMTnxjBZT/mhNaYm0EX/R9wdjHHv16", "CLIENT"),
    ("brett@example.com", "brett", "{bcrypt}$2b$10$bk5uMLVYNIaXnUGK1sgVB.f627Sc02Jv3ZcOYDlEfwM9YkwqNeFFq", "CLIENT"),
    ("tyler@example.com", "tyler", "{bcrypt}$2b$10$/cf7fGxSWoMxWgLzD/rG8eSzgjja5BRIs0j73jUNASxI8IGeWG1O.", "CLIENT"),
    ("jenn@example.com", "jenn", "{bcrypt}$2b$10$6pGnky8Q1MDWMZ3qWI1Q9.P3hAw0Q8zVTMXX1zJd6x4Wipa5/wTsC", "CLIENT"),
    ("ray@example.com", "ray", "{bcrypt}$2b$10$DSSLDzrbnfk0ay01rxfaE.aPdFfay1q5SGDBCHxHjCX8pNCry5zgS", "CLIENT"),
    ("tourpro@example.com", "tourpro", "{bcrypt}$2b$10$vodwi/TPv8YPmKCVAAUv5.q/gIzF2hplVfzyem02HCRq.oN3PF3Ga", "HOST");

insert into destination (name, region, description, latitude, longitude) values
    ("Pigeon Forge", "Tennessee", "Smoky Mountain foothills. Cheap beds and the drive in is half the trip.", 35.788400, -83.554300),
    ("Branson", "Missouri", "Ozark hills, short drives between courses, and midweek rooms that cost nothing.", 36.643700, -93.218500),
    ("Oklahoma City", "Oklahoma", "City munis that play better than they price. The cheapest golf on this list.", 35.467600, -97.516400),
    ("Bowling Green", "Kentucky", "Rolling Kentucky farmland, and nobody on the tee sheet on a Tuesday.", 36.968500, -86.480800),
    ("Hot Springs", "Arkansas", "Mountain golf, hot baths afterwards, and prices from another decade.", 34.503700, -93.055200),
    ("Jekyll Island", "Georgia", "Three state-run courses on a barrier island. Big groups, small money.", 31.069900, -81.412600),
    ("Myrtle Beach", "South Carolina", "Ninety-odd courses and the package deal that invented the buddies trip.", 33.689100, -78.886700),
    ("Biloxi", "Mississippi", "Gulf coast tracks bundled with casino rooms. Hard to beat on price.", 30.396000, -88.885300),
    ("Gulf Shores", "Alabama", "Beach in the morning, golf in the afternoon, or the other way round.", 30.246000, -87.700800),
    ("Gaylord", "Michigan", "Michigan's golf mecca. Shoulder season is when the rates make sense.", 45.027500, -84.674800),
    ("Prattville", "Alabama", "Robert Trent Jones Trail. Championship golf at municipal money.", 32.464000, -86.459700),
    ("Southern Pines", "North Carolina", "The Sandhills without the Pinehurst price tag. Same sand, same architects.", 35.174300, -79.392200),
    ("Lake of the Ozarks", "Missouri", "Lake country. Everything is a boat ride or a ten-minute drive.", 38.198900, -92.636300),
    ("Ocean City", "Maryland", "Big-group country. Courses along the coast, condos by the dozen.", 38.336500, -75.084900),
    ("Wisconsin Dells", "Wisconsin", "Two very good courses inside a town built entirely for groups.", 43.627500, -89.771000),
    ("Tucson", "Arizona", "Desert golf at half the Scottsdale rate, with better mountains.", 32.222600, -110.974700),
    ("Las Vegas", "Nevada", "Cheap flights, cheap rooms midweek, and tee times at every hour.", 36.169900, -115.139800),
    ("Atlantic City", "New Jersey", "Pine barrens golf twenty minutes from the boardwalk.", 39.364300, -74.422900),
    ("Orlando", "Florida", "More courses than anywhere in America, and summer rates to match.", 28.538300, -81.379200),
    ("Galena", "Illinois", "Driftless-area hills nobody expects in Illinois.", 42.416700, -90.429000),
    ("Traverse City", "Michigan", "Cherry country. Water views from half the tee boxes.", 44.763100, -85.620600),
    ("Hilton Head", "South Carolina", "Lowcountry resort golf. Book the second-tier courses and it is affordable.", 32.216300, -80.752600),
    ("Destin", "Florida", "Emerald coast. Golf is the excuse; the beach is the reason.", 30.393500, -86.495800),
    ("Scottsdale", "Arizona", "Desert target golf. Come in summer and it costs a third of winter.", 33.494200, -111.926100),
    ("San Antonio", "Texas", "Hill country golf with a city attached. Good rates outside spring.", 29.424100, -98.493600),
    ("Pinehurst", "North Carolina", "Nine courses in one village. No. 2 is the draw, the others are the value.", 35.195400, -79.469500),
    ("French Lick", "Indiana", "Two historic courses in the middle of nowhere, which is why it is affordable.", 38.549500, -86.620000),
    ("Charleston", "South Carolina", "Marsh golf, and the best food of any destination on this list.", 32.776500, -79.931100),
    ("Bend", "Oregon", "High desert, volcanic soil, and the driest summer in Oregon.", 44.058200, -121.315300),
    ("Palm Springs", "California", "A hundred courses in one valley. Summer rates are a genuine bargain.", 33.830300, -116.545300),
    ("Kohler", "Wisconsin", "Whistling Straits and Blackwolf Run. Two days, one splurge.", 43.739700, -87.782600),
    ("Sunriver", "Oregon", "Four courses inside one resort. Walk or bike between them.", 43.877900, -121.438100),
    ("Colorado Springs", "Colorado", "Front range views and altitude that adds thirty yards.", 38.833900, -104.821400),
    ("Park City", "Utah", "Mountain golf at 7,000 feet. The ball goes a long way.", 40.646100, -111.498000),
    ("Sea Island", "Georgia", "Georgia coast at its most polished. The splurge of the Southeast.", 31.195800, -81.339000),
    ("San Diego", "California", "Perfect weather every month, and Torrey Pines on a public tee sheet.", 32.715700, -117.161100),
    ("Innisbrook", "Florida", "Copperhead is a PGA Tour stop you can actually book.", 28.112200, -82.707900),
    ("Bandon Dunes", "Oregon", "Walking-only links on the Pacific. Shoulder season halves the rate.", 43.189000, -124.392000),
    ("Monterey", "California", "The peninsula. Play the public tracks and stay in town to afford it.", 36.600200, -121.894700),
    ("Kiawah Island", "South Carolina", "Five resort courses. The Ocean Course is famous, the others are the trip.", 32.608000, -80.085000);

insert into trip_request
    (user_id, destination_id, budget_per_player, player_count, rounds_requested,
     nights, earliest_start, latest_start, notes, status, decline_reason, created_at) values
    (1, 1, 125.00, 2, 1, 1, "2026-07-20", "2026-07-20", null, "BOOKED", null, "2026-06-26 10:00:00"),
    (2, 2, 125.00, 4, 2, 1, "2026-07-09", "2026-07-09", null, "BOOKED", null, "2026-06-15 10:00:00"),
    (3, 3, 125.00, 6, 2, 1, "2026-06-28", "2026-06-28", null, "BOOKED", null, "2026-06-04 10:00:00"),
    (4, 4, 150.00, 4, 2, 1, "2026-06-17", "2026-06-17", null, "BOOKED", null, "2026-05-24 10:00:00"),
    (5, 5, 150.00, 4, 2, 1, "2026-06-06", "2026-06-06", null, "BOOKED", null, "2026-05-13 10:00:00"),
    (6, 6, 200.00, 8, 2, 1, "2026-05-26", "2026-05-26", null, "BOOKED", null, "2026-05-02 10:00:00"),
    (7, 7, 225.00, 4, 2, 2, "2026-05-15", "2026-05-15", null, "BOOKED", null, "2026-04-21 10:00:00"),
    (8, 8, 225.00, 6, 2, 2, "2026-05-04", "2026-05-04", null, "BOOKED", null, "2026-04-10 10:00:00"),
    (1, 9, 250.00, 4, 2, 2, "2026-04-23", "2026-04-23", null, "BOOKED", null, "2026-03-30 10:00:00"),
    (2, 10, 250.00, 4, 2, 2, "2026-04-12", "2026-04-12", null, "BOOKED", null, "2026-03-19 10:00:00"),
    (3, 1, 275.00, 6, 3, 2, "2026-04-01", "2026-04-01", null, "BOOKED", null, "2026-03-08 10:00:00"),
    (4, 11, 300.00, 6, 3, 1, "2026-03-21", "2026-03-21", null, "BOOKED", null, "2026-02-25 10:00:00"),
    (5, 12, 325.00, 4, 2, 2, "2026-03-10", "2026-03-10", null, "BOOKED", null, "2026-02-14 10:00:00"),
    (6, 7, 350.00, 4, 3, 2, "2026-02-27", "2026-02-27", null, "BOOKED", null, "2026-02-03 10:00:00"),
    (7, 13, 350.00, 6, 3, 1, "2026-02-16", "2026-02-16", null, "BOOKED", null, "2026-01-23 10:00:00"),
    (8, 14, 375.00, 8, 3, 2, "2026-02-05", "2026-02-05", null, "BOOKED", null, "2026-01-12 10:00:00"),
    (1, 15, 375.00, 4, 2, 2, "2026-01-25", "2026-01-25", null, "BOOKED", null, "2026-01-01 10:00:00"),
    (2, 16, 400.00, 4, 3, 2, "2026-01-14", "2026-01-14", null, "BOOKED", null, "2025-12-21 10:00:00"),
    (3, 17, 425.00, 6, 3, 2, "2026-01-03", "2026-01-03", null, "BOOKED", null, "2025-12-10 10:00:00"),
    (4, 18, 450.00, 4, 3, 2, "2025-12-23", "2025-12-23", null, "BOOKED", null, "2025-11-29 10:00:00"),
    (5, 2, 450.00, 4, 3, 2, "2025-12-12", "2025-12-12", null, "BOOKED", null, "2025-11-18 10:00:00"),
    (6, 19, 475.00, 12, 3, 2, "2025-12-01", "2025-12-01", null, "BOOKED", null, "2025-11-07 10:00:00"),
    (7, 20, 475.00, 6, 3, 2, "2025-11-20", "2025-11-20", null, "BOOKED", null, "2025-10-27 10:00:00"),
    (8, 21, 500.00, 4, 3, 2, "2025-11-09", "2025-11-09", null, "BOOKED", null, "2025-10-16 10:00:00"),
    (1, 10, 525.00, 6, 3, 2, "2025-10-29", "2025-10-29", null, "BOOKED", null, "2025-10-05 10:00:00"),
    (2, 7, 525.00, 8, 4, 3, "2025-10-18", "2025-10-18", null, "BOOKED", null, "2025-09-24 10:00:00"),
    (3, 22, 525.00, 4, 3, 2, "2025-10-07", "2025-10-07", null, "BOOKED", null, "2025-09-13 10:00:00"),
    (4, 23, 550.00, 4, 3, 2, "2025-09-26", "2025-09-26", null, "BOOKED", null, "2025-09-02 10:00:00"),
    (5, 24, 550.00, 4, 3, 3, "2025-09-15", "2025-09-15", null, "BOOKED", null, "2025-08-22 10:00:00"),
    (6, 14, 575.00, 6, 3, 3, "2025-09-04", "2025-09-04", null, "BOOKED", null, "2025-08-11 10:00:00"),
    (7, 25, 625.00, 6, 3, 2, "2025-08-24", "2025-08-24", null, "BOOKED", null, "2025-07-31 10:00:00"),
    (8, 26, 625.00, 4, 3, 2, "2025-08-13", "2025-08-13", null, "BOOKED", null, "2025-07-20 10:00:00"),
    (1, 27, 650.00, 4, 2, 2, "2025-08-02", "2025-08-02", null, "BOOKED", null, "2025-07-09 10:00:00"),
    (2, 28, 650.00, 4, 3, 2, "2025-07-22", "2025-07-22", null, "BOOKED", null, "2025-06-28 10:00:00"),
    (3, 29, 650.00, 4, 3, 2, "2025-07-11", "2025-07-11", null, "BOOKED", null, "2025-06-17 10:00:00"),
    (4, 30, 675.00, 4, 3, 2, "2025-06-30", "2025-06-30", null, "BOOKED", null, "2025-06-06 10:00:00"),
    (5, 31, 725.00, 2, 1, 2, "2025-06-19", "2025-06-19", null, "BOOKED", null, "2025-05-26 10:00:00"),
    (6, 7, 750.00, 4, 3, 3, "2025-06-08", "2025-06-08", null, "BOOKED", null, "2025-05-15 10:00:00"),
    (7, 32, 775.00, 4, 4, 2, "2025-05-28", "2025-05-28", null, "BOOKED", null, "2025-05-04 10:00:00"),
    (8, 33, 775.00, 6, 3, 3, "2025-05-17", "2025-05-17", null, "BOOKED", null, "2025-04-23 10:00:00"),
    (1, 34, 800.00, 4, 3, 3, "2025-05-06", "2025-05-06", null, "BOOKED", null, "2025-04-12 10:00:00"),
    (2, 35, 850.00, 2, 2, 2, "2025-04-25", "2025-04-25", null, "BOOKED", null, "2025-04-01 10:00:00"),
    (3, 26, 850.00, 6, 3, 3, "2025-04-14", "2025-04-14", null, "BOOKED", null, "2025-03-21 10:00:00"),
    (4, 36, 850.00, 4, 3, 3, "2025-04-03", "2025-04-03", null, "BOOKED", null, "2025-03-10 10:00:00"),
    (5, 22, 875.00, 4, 3, 3, "2025-03-23", "2025-03-23", null, "BOOKED", null, "2025-02-27 10:00:00"),
    (6, 24, 875.00, 4, 3, 2, "2025-03-12", "2025-03-12", null, "BOOKED", null, "2025-02-16 10:00:00"),
    (7, 37, 950.00, 6, 3, 3, "2025-03-01", "2025-03-01", null, "BOOKED", null, "2025-02-05 10:00:00"),
    (8, 38, 950.00, 4, 2, 2, "2025-02-18", "2025-02-18", null, "BOOKED", null, "2025-01-25 10:00:00"),
    (1, 39, 950.00, 4, 2, 2, "2025-02-07", "2025-02-07", null, "BOOKED", null, "2025-01-14 10:00:00"),
    (2, 40, 975.00, 4, 3, 2, "2025-01-27", "2025-01-27", null, "BOOKED", null, "2025-01-03 10:00:00"),
    (1, 2, 475.00, 4, 3, 2, "2026-09-25", "2026-10-02", "Driving from St. Louis, keep it close to town", "PROPOSED", null, "2026-08-26 10:00:00"),
    (2, 10, 550.00, 6, 3, 2, "2026-09-18", "2026-09-27", "Six of us, two carts, one argument guaranteed", "PENDING", null, "2026-08-19 10:00:00"),
    (3, 7, 400.00, 8, 3, 2, "2026-10-09", "2026-10-09", "Bachelor party. Cheap and cheerful.", "PENDING", null, "2026-09-09 10:00:00"),
    (4, 15, 425.00, 4, 2, 2, "2026-10-02", "2026-10-24", null, "PENDING", null, "2026-09-02 10:00:00"),
    (5, 5, 350.00, 4, 3, 1, "2026-09-12", "2026-09-26", "One night only, we all have Sunday obligations", "PENDING", null, "2026-08-13 10:00:00"),
    (6, 14, 500.00, 12, 2, 2, "2026-09-30", "2026-10-17", "Twelve of us. Yes, twelve.", "PENDING", null, "2026-08-31 10:00:00"),
    (7, 26, 250.00, 4, 3, 2, "2026-11-06", "2026-11-30", "Heard the Sandhills is doable off-season", "DECLINED", "Three rounds plus two nights in the Sandhills runs closer to $450 a player, even in November.", "2026-10-07 10:00:00"),
    (8, 40, 300.00, 4, 3, 2, "2026-10-23", "2026-11-06", "Saw a $955 trip on here, hoping for cheaper", "DECLINED", "Kiawah's resort courses start around $215 a round. Try Charleston National or Patriots Point instead.", "2026-09-23 10:00:00"),
    (3, 24, 200.00, 4, 3, 2, "2026-12-04", "2026-12-04", null, "DECLINED", "December is peak in the desert. $200 a player will not cover one round, let alone three.", "2026-11-04 10:00:00");

insert into booking
    (request_id, courses, lodging, start_date, end_date, total_cost, itinerary, booked_by,
     booked_at, confirmed_at) values
    (1, "Gatlinburg Golf Course", "Riveredge Motor Lodge", "2026-07-20", "2026-07-21", 220.00, "1 round over 2 days. Gatlinburg Golf Course last.", 9, "2026-06-30 14:00:00", "2026-06-30 14:00:00"),
    (2, "Lockwood Municipal\nCameron Golf Course", "Best Western Center Pointe", "2026-07-09", "2026-07-10", 476.00, "2 rounds over 2 days. Cameron Golf Course last.", 9, "2026-06-19 14:00:00", "2026-06-19 14:00:00"),
    (3, "Lincoln Park East\nEarlywine Park South", "Best Western Plus OKC", "2026-06-28", "2026-06-29", 744.00, "2 rounds over 2 days. Earlywine Park South last.", 9, "2026-06-08 14:00:00", "2026-06-08 14:00:00"),
    (4, "Crosswinds\nPaul Walker Municipal", "Holiday Inn University Plaza", "2026-06-17", "2026-06-18", 544.00, "2 rounds over 2 days. Paul Walker Municipal last.", 9, "2026-05-28 14:00:00", "2026-05-28 14:00:00"),
    (5, "Glenwood Country Club\nCooper Communities Red Apple", "Quality Inn Hot Springs", "2026-06-06", "2026-06-07", 592.00, "2 rounds over 2 days. Cooper Communities Red Apple last.", 9, "2026-05-17 14:00:00", "2026-05-17 14:00:00"),
    (6, "Pine Lakes\nIndian Mound", "Beachview Club", "2026-05-26", "2026-05-27", 1424.00, "2 rounds over 2 days. Indian Mound last.", 9, "2026-05-06 14:00:00", "2026-05-06 14:00:00"),
    (7, "Man O'War\nBlackmoor", "Sand Dunes Resort", "2026-05-15", "2026-05-17", 824.00, "2 rounds over 3 days. Blackmoor last.", 9, "2026-04-25 14:00:00", "2026-04-25 14:00:00"),
    (8, "Shell Landing\nThe Preserve", "Island View Casino", "2026-05-04", "2026-05-06", 1272.00, "2 rounds over 3 days. The Preserve last.", 9, "2026-04-14 14:00:00", "2026-04-14 14:00:00"),
    (9, "Gulf Shores Golf Club\nCraft Farms Cotton Creek", "Beachside Resort", "2026-04-23", "2026-04-25", 912.00, "2 rounds over 3 days. Craft Farms Cotton Creek last.", 9, "2026-04-03 14:00:00", "2026-04-03 14:00:00"),
    (10, "The Loon\nBlack Bear", "Hampton Inn Gaylord", "2026-04-12", "2026-04-14", 960.00, "2 rounds over 3 days. Black Bear last.", 9, "2026-03-23 14:00:00", "2026-03-23 14:00:00"),
    (11, "Bent Creek\nSevierville GC Highlands\nGatlinburg GC", "Music Road Resort", "2026-04-01", "2026-04-03", 1620.00, "3 rounds over 3 days. Gatlinburg GC last.", 9, "2026-03-12 14:00:00", "2026-03-12 14:00:00"),
    (12, "RTJ Capitol Hill Judge\nSenator\nLegislator", "Marriott Prattville", "2026-03-21", "2026-03-22", 1668.00, "3 rounds over 2 days. Legislator last.", 9, "2026-03-01 14:00:00", "2026-03-01 14:00:00"),
    (13, "Southern Pines GC\nLongleaf", "Springhill Suites Southern Pines", "2026-03-10", "2026-03-12", 1256.00, "2 rounds over 3 days. Longleaf last.", 9, "2026-02-18 14:00:00", "2026-02-18 14:00:00"),
    (14, "Man O'War\nLitchfield CC\nBlackmoor", "Ocean Reef Resort", "2026-02-27", "2026-03-01", 1328.00, "3 rounds over 3 days. Blackmoor last.", 9, "2026-02-07 14:00:00", "2026-02-07 14:00:00"),
    (15, "Old Kinderhook\nThe Cove\nSycamore Creek", "Old Kinderhook Lodge", "2026-02-16", "2026-02-17", 2016.00, "3 rounds over 2 days. Sycamore Creek last.", 9, "2026-01-27 14:00:00", "2026-01-27 14:00:00"),
    (16, "Eagle's Landing\nRum Pointe\nGlenRiddle War Admiral", "Carousel Resort", "2026-02-05", "2026-02-07", 2864.00, "3 rounds over 3 days. GlenRiddle War Admiral last.", 9, "2026-01-16 14:00:00", "2026-01-16 14:00:00"),
    (17, "Trappers Turn\nWild Rock", "Chula Vista Resort", "2026-01-25", "2026-01-27", 1440.00, "2 rounds over 3 days. Wild Rock last.", 9, "2026-01-05 14:00:00", "2026-01-05 14:00:00"),
    (18, "Del Lago\nArizona National\nStarr Pass", "Doubletree Tucson", "2026-01-14", "2026-01-16", 1524.00, "3 rounds over 3 days. Starr Pass last.", 9, "2025-12-25 14:00:00", "2025-12-25 14:00:00"),
    (19, "Angel Park Palm\nRhodes Ranch\nBali Hai", "Orleans Hotel", "2026-01-03", "2026-01-05", 2490.00, "3 rounds over 3 days. Bali Hai last.", 9, "2025-12-14 14:00:00", "2025-12-14 14:00:00"),
    (20, "Blue Heron Pines\nTwisted Dune\nHarbor Pines", "Resorts Casino Hotel", "2025-12-23", "2025-12-25", 1716.00, "3 rounds over 3 days. Harbor Pines last.", 9, "2025-12-03 14:00:00", "2025-12-03 14:00:00"),
    (21, "Pointe Royale\nThousand Hills\nBranson Hills", "Still Waters Resort", "2025-12-12", "2025-12-14", 1760.00, "3 rounds over 3 days. Branson Hills last.", 9, "2025-11-22 14:00:00", "2025-11-22 14:00:00"),
    (22, "Falcon's Fire\nCelebration\nOrange County National Panther", "Rosen Shingle Creek", "2025-12-01", "2025-12-03", 5448.00, "3 rounds over 3 days. Orange County National Panther last.", 9, "2025-11-11 14:00:00", "2025-11-11 14:00:00"),
    (23, "Eagle Ridge North\nEagle Ridge South\nEagle Ridge General", "Eagle Ridge Resort", "2025-11-20", "2025-11-22", 2766.00, "3 rounds over 3 days. Eagle Ridge General last.", 9, "2025-10-31 14:00:00", "2025-10-31 14:00:00"),
    (24, "A-Ga-Ming Torch\nGrand Traverse Wolverine\nLochenHeath", "Grand Traverse Resort", "2025-11-09", "2025-11-11", 1912.00, "3 rounds over 3 days. LochenHeath last.", 9, "2025-10-20 14:00:00", "2025-10-20 14:00:00"),
    (25, "Treetops Smith Signature\nThe Loon\nBlack Bear", "Treetops Resort", "2025-10-29", "2025-10-31", 3030.00, "3 rounds over 3 days. Black Bear last.", 9, "2025-10-09 14:00:00", "2025-10-09 14:00:00"),
    (26, "Arrowhead\nTradition Club\nBlackmoor\nMan O'War", "Beach Cove Resort", "2025-10-18", "2025-10-21", 4056.00, "4 rounds over 4 days. Man O'War last.", 9, "2025-09-28 14:00:00", "2025-09-28 14:00:00"),
    (27, "Oyster Reef\nPalmetto Dunes Fazio\nPalmetto Dunes RTJ", "Beach House Resort", "2025-10-07", "2025-10-09", 2044.00, "3 rounds over 3 days. Palmetto Dunes RTJ last.", 9, "2025-09-17 14:00:00", "2025-09-17 14:00:00"),
    (28, "Regatta Bay\nKelly Plantation\nShark's Tooth", "Holiday Inn Express Destin", "2025-09-26", "2025-09-28", 2140.00, "3 rounds over 3 days. Shark's Tooth last.", 9, "2025-09-06 14:00:00", "2025-09-06 14:00:00"),
    (29, "We-Ko-Pa Saguaro\nGrayhawk Talon\nRaptor", "Scottsdale Marriott", "2025-09-15", "2025-09-18", 2160.00, "3 rounds over 4 days. Raptor last.", 9, "2025-08-26 14:00:00", "2025-08-26 14:00:00"),
    (30, "GlenRiddle Man O'War\nLighthouse Sound\nRum Pointe", "Princess Royale", "2025-09-04", "2025-09-07", 3420.00, "3 rounds over 4 days. Rum Pointe last.", 9, "2025-08-15 14:00:00", "2025-08-15 14:00:00"),
    (31, "TPC San Antonio Canyons\nLa Cantera Palmer\nBrackenridge Park", "Drury Plaza Riverwalk", "2025-08-24", "2025-08-26", 3690.00, "3 rounds over 3 days. Brackenridge Park last.", 9, "2025-08-04 14:00:00", "2025-08-04 14:00:00"),
    (32, "Pinehurst No. 4\nMid Pines\nLongleaf", "Springhill Suites Pinehurst", "2025-08-13", "2025-08-15", 2500.00, "3 rounds over 3 days. Longleaf last.", 9, "2025-07-24 14:00:00", "2025-07-24 14:00:00"),
    (33, "Donald Ross Course\nFrench Lick Valley", "French Lick Springs Hotel", "2025-08-02", "2025-08-04", 2560.00, "2 rounds over 3 days. French Lick Valley last.", 9, "2025-07-13 14:00:00", "2025-07-13 14:00:00"),
    (34, "Charleston National\nWild Dunes Links\nPatriots Point", "Wild Dunes Resort", "2025-07-22", "2025-07-24", 2564.00, "3 rounds over 3 days. Patriots Point last.", 9, "2025-07-02 14:00:00", "2025-07-02 14:00:00"),
    (35, "Tetherow\nWidgi Creek\nJuniper", "Riverhouse on the Deschutes", "2025-07-11", "2025-07-13", 2580.00, "3 rounds over 3 days. Juniper last.", 9, "2025-06-21 14:00:00", "2025-06-21 14:00:00"),
    (36, "Desert Willow Firecliff\nIndian Wells Celebrity\nEscena", "Westin Mission Hills", "2025-06-30", "2025-07-02", 2660.00, "3 rounds over 3 days. Escena last.", 9, "2025-06-10 14:00:00", "2025-06-10 14:00:00"),
    (37, "Whistling Straits Irish", "Inn on Woodlake", "2025-06-19", "2025-06-21", 1450.00, "1 round over 3 days. Whistling Straits Irish last.", 9, "2025-05-30 14:00:00", "2025-05-30 14:00:00"),
    (38, "Barefoot Dye\nBarefoot Love\nTPC Myrtle Beach", "Barefoot Resort Villas", "2025-06-08", "2025-06-11", 3000.00, "3 rounds over 4 days. TPC Myrtle Beach last.", 9, "2025-05-19 14:00:00", "2025-05-19 14:00:00"),
    (39, "Crosswater\nMeadows\nWoodlands\nCaldera Links", "Sunriver Resort", "2025-05-28", "2025-05-30", 3008.00, "4 rounds over 3 days. Caldera Links last.", 9, "2025-05-08 14:00:00", "2025-05-08 14:00:00"),
    (40, "Antler Creek\nKing's Deer\nCheyenne Shadows", "Cheyenne Mountain Resort", "2025-05-17", "2025-05-20", 4626.00, "3 rounds over 4 days. Cheyenne Shadows last.", 9, "2025-04-27 14:00:00", "2025-04-27 14:00:00"),
    (41, "Soldier Hollow Gold\nWasatch Mountain Lake\nPark City GC", "Park City Peaks Hotel", "2025-05-06", "2025-05-09", 3120.00, "3 rounds over 4 days. Park City GC last.", 9, "2025-04-16 14:00:00", "2025-04-16 14:00:00"),
    (42, "Retreat\nPlantation", "The Inn at Sea Island", "2025-04-25", "2025-04-27", 1660.00, "2 rounds over 3 days. Plantation last.", 9, "2025-04-05 14:00:00", "2025-04-05 14:00:00"),
    (43, "Pinehurst No. 3\nPine Needles\nMid Pines", "Pinehurst Carolina Hotel", "2025-04-14", "2025-04-17", 5040.00, "3 rounds over 4 days. Mid Pines last.", 9, "2025-03-25 14:00:00", "2025-03-25 14:00:00"),
    (44, "Torrey Pines North\nCoronado Municipal\nMaderas", "Bahia Resort Hotel", "2025-04-03", "2025-04-06", 3396.00, "3 rounds over 4 days. Maderas last.", 9, "2025-03-14 14:00:00", "2025-03-14 14:00:00"),
    (45, "Heron Point\nAtlantic Dunes\nPalmetto Dunes RTJ", "Sea Pines Resort", "2025-03-23", "2025-03-26", 3480.00, "3 rounds over 4 days. Palmetto Dunes RTJ last.", 9, "2025-03-03 14:00:00", "2025-03-03 14:00:00"),
    (46, "TPC Scottsdale Champions\nTroon North Monument\nWe-Ko-Pa Cholla", "The Phoenician", "2025-03-12", "2025-03-14", 3500.00, "3 rounds over 3 days. We-Ko-Pa Cholla last.", 9, "2025-02-20 14:00:00", "2025-02-20 14:00:00"),
    (47, "Copperhead\nIsland\nNorth", "Innisbrook Resort", "2025-03-01", "2025-03-04", 5580.00, "3 rounds over 4 days. North last.", 9, "2025-02-09 14:00:00", "2025-02-09 14:00:00"),
    (48, "Bandon Dunes\nPacific Dunes", "Lily Pond Lodging", "2025-02-18", "2025-02-20", 3760.00, "2 rounds over 3 days. Pacific Dunes last.", 9, "2025-01-29 14:00:00", "2025-01-29 14:00:00"),
    (49, "Pasatiempo\nPoppy Hills", "Casa Munras Monterey", "2025-02-07", "2025-02-09", 3760.00, "2 rounds over 3 days. Poppy Hills last.", 9, "2025-01-18 14:00:00", "2025-01-18 14:00:00"),
    (50, "Turtle Point\nOsprey Point\nCougar Point", "Kiawah Island Inn", "2025-01-27", "2025-01-29", 3820.00, "3 rounds over 3 days. Cougar Point last.", 9, "2025-01-07 14:00:00", "2025-01-07 14:00:00"),
    (51, "Payne's Valley\nBuffalo Ridge\nOzarks National", "Big Cedar Lodge",
     "2026-09-25", "2026-09-27", 2100.00, "3 rounds over 3 days. Ozarks National last.", 9,
     "2026-08-27 14:00:00", null);

/*
 * A proposal request 51 already turned down, so the host's history thread has something
 * to show on a first launch. Older than the live one above, which is what keeps
 * findLiveProposal returning the live row rather than this one.
 *
 * It is inside the client's budget and was still refused -- which is the whole argument
 * for proposing rather than booking. Price was never the only thing that could be wrong.
 */
insert into booking
    (request_id, courses, lodging, start_date, end_date, total_cost, itinerary, booked_by,
     booked_at, confirmed_at, countered_at, counter_reason, counter_note) values
    (51, "Top of the Rock\nMountain Top", "Angler's Lodge",
     "2026-09-28", "2026-09-30", 1980.00, "2 rounds over 3 days. Mountain Top last.", 9,
     "2026-08-26 14:00:00", null, "2026-08-26 19:20:00", "COURSES",
     "Two of us have played Top of the Rock twice this year. Anything but that one.");

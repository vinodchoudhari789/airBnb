# Open Items — Backend Performance Work

Deferred decisions and follow-ups raised while implementing the performance
analysis recommendations. Not yet actioned; revisit when convenient.

---

## 1. IDENTITY generation strategy blocks insert batching (Inventory, Guest)

**Status:** Open — deliberately deferred, existing schema/data untouched for now.

**Context:** All entities use `@GeneratedValue(strategy = GenerationType.IDENTITY)`.
Hibernate cannot batch *new-row inserts* under `IDENTITY` because it needs each
row's generated PK back before it can process the next entity, so inserts are
sent one statement at a time regardless of `hibernate.jdbc.batch_size`.

This affects two of the doc's P1 bulk-insert targets specifically:
- **Inventory initialization** (rooms × days on hotel activation) — still row-by-row.
- **Guest inserts** (multi-guest bookings) — still row-by-row.

Batching *does* work today for update-heavy paths (`PricingUpdateService`'s
`saveAll()` calls on existing rows), since those entities already have IDs.

**Options when we revisit:**
- Switch `Inventory` and `Guest` (only the entities actually bulk-inserted) to
  `GenerationType.SEQUENCE` with a pooled/pooled-lo optimizer — enables true
  insert batching, but is a schema migration (new sequence, ID column default)
  and needs care around existing data / any code assuming IDENTITY semantics.
- Leave as `IDENTITY` and rely on `reWriteBatchedInserts=true` (already added)
  as a partial mitigation — pgjdbc can still coalesce statements at the driver
  level even when Hibernate issues them individually, though this is weaker
  than real batching.
- Do nothing further if bulk-insert volume turns out not to be a real
  bottleneck once measured (see verification step below).

**How to check if it matters before deciding:** enable SQL logging /
`hibernate.generate_statistics` (already on) around a hotel activation with
~20 rooms × 365 days, and look at actual insert statement count / timing.

---

## 2. `loadCurrentBooking` fallback caps at 100 bookings

**Status:** Open — pragmatic workaround in place, not a full fix.

**Context:** `MyBookingsPage`'s "resume an in-progress booking after refresh"
fallback (`bookingsSlice.loadCurrentBooking`) previously called
`GET /users/myBookings` with no pagination and scanned the full list for a
matching booking id. Now that this endpoint is paginated (and `take` is
clamped to 100 server-side, see item 3 below), the fallback fetches
`take=100` and searches within that page - fine for virtually all users, but
a user with >100 bookings whose in-progress booking happens to be older than
their most recent 100 would hit "Booking not found" on refresh.

**Proper fix:** add a dedicated `GET /bookings/{id}` endpoint (scoped to the
current user) so this fallback doesn't depend on pagination limits at all.
Deferred since it's backend scope beyond the original pagination/report task.

## 3. No upper bound on `take` query param

**Status:** ✅ Resolved.

`GET /admin/hotels/{hotelId}/bookings` and `GET /users/myBookings` now clamp
`take` to a max of 100 server-side (`BookingServiceImpl.toPageable`,
`MAX_PAGE_SIZE = 100`), regardless of what a client requests. Frontend's
`take=100` fallback fetch (item 2 above) was aligned to match this cap.

---

## 4. [SECURITY] Entity-in-DTO leaked owner credentials + guest's own password hash

**Status:** ✅ Resolved.

**Context:** `HotelPriceDTO.hotel` held the full `Hotel` entity, which has an
eager `@ManyToOne owner` (`User`) field with no `@JsonIgnore`. `GET
/hotels/search` is fully unauthenticated (`WebSecurityConfig` -
`/hotels/**` falls under `anyRequest().permitAll()`), so this endpoint was
serving the hotel owner's **email and bcrypt password hash** to anyone,
unauthenticated, in every search result.

Separately, `GuestDTO.user` held the full `User` entity. Since `GuestDTO`
nests inside `BookingDTO.guestSet` (returned from every booking-fetching
endpoint), and `Guest.user` also has no `@JsonIgnore`, this leaked the
current user's own email/password hash back to themselves on every booking
fetch. Lower severity (self-directed, authenticated-only) but still a
should-never-happen anti-pattern - password hashes should never leave the
server, full stop.

**Fix:**
- `HotelPriceDTO.hotel` changed from `Hotel` to a new `HotelSummaryDTO`
  (`id, name, city, photos, amenities` - exactly what the search UI renders,
  nothing else). JSON shape (`{ hotel: {...}, price }`) unchanged, so no
  frontend changes were needed.
- `HotelMinPriceRepository.findHotelWithAvailableInventory` now projects
  those flat fields directly (via a new `HotelSearchRowDTO`, since JPQL
  constructor expressions can't nest `new` calls) instead of selecting the
  whole `Hotel` entity. `InventoryServiceImpl.searchHotels` remaps each row
  into the nested `HotelPriceDTO` shape.
- `GuestDTO.user` removed entirely - it was dead on input already (always
  overwritten server-side by `guest.setUser(getCurrentUser())` in
  `BookingServiceImpl`) and is simply gone from responses now.

**Not yet verified:** couldn't run `mvn test`/`mvn compile` in this sandbox
(no Maven/network access here). The `String[]` (`photos`/`amenities`,
`TEXT[]` columns) being selectable as individual JPQL constructor-expression
arguments should work the same as it does today via the full entity fetch,
but this should be confirmed with a real build before merging.

---

# Backend changes — index

Drop these files straight into your project at the matching path under
`src/main/java/com/project/airBnbApp/...` (overwrite the existing ones).

## Files included in this zip (full, ready to copy in)

0.9999 **⚠️ New rule: name, city, and all contact-info fields are locked
   after hotel creation.** Settable only when creating a hotel; any attempt
   to change name, city, address, location, email, or phoneNumber
   afterward is silently ignored server-side (same defense-in-depth
   pattern already used for `active`), since existing/past bookings rely
   on this info staying accurate. Only `amenities` and `photos` remain
   editable after creation - flagged so you know this also means a manager
   can never fix a typo in the name or update a changed phone number
   through this endpoint; that's the explicit tradeoff you asked for.
   - **service/HotelServiceImpl.java** — `updateHotelById` now captures and
     restores all 6 fields after the blanket ModelMapper call, same
     pattern already used for `active`.
   - Frontend: `HotelForm.jsx` disables all 6 inputs (with an explanatory
     note) once editing an existing hotel, but leaves them editable at
     creation time.

0.999 **⚠️ Bug fix: editing a room's price didn't update search results or
   the admin's own inventory table for up to an hour.** The actual booking
   amount and guest-facing price preview were always correct (they compute
   live from `room.basePrice`), but the *cached* `Inventory.price` column
   and the `hotel_min_price` table (search's "from ₹X/night") were only
   refreshed by an hourly scheduled job - there was literally a
   `// TODO: if price or inventory is updated then update the inventory for
   this room.` left in the code. Fixed by reusing the scheduler's own
   refresh logic immediately after a room edit:
   - **service/PricingUpdateService.java** — `updateHotelPrices(Hotel)`
     changed from `private` to `public` so it can be called on demand, not
     just from the hourly `@Scheduled` job. No behavior change to the
     scheduler itself.
   - **service/RoomServiceImpl.java** — `updateRoomById` now calls
     `pricingUpdateService.updateHotelPrices(room.getHotel())` right after
     saving the room, immediately refreshing that hotel's cached inventory
     prices and search min-price. Cheap (scoped to one hotel) and a safe
     no-op if the hotel has no inventory yet.

0.995 **⚠️⚠️ Bug fix: inventory table showed nothing despite the API
   returning real data.** `dto/InventoryDTO.java` declared `date` as
   `java.util.Date`, but the actual `Inventory` entity field is
   `java.time.LocalDate` — an incompatible type pair ModelMapper can't
   cleanly convert, so `date` silently came through as unusable/null on
   every row. The frontend's date-range filter then compared against
   garbage and matched nothing, even though the array itself was full.
   Fixed by changing the DTO field to `LocalDate`, matching the entity (same
   type every other date field in this codebase already uses, e.g.
   `BookingDTO.checkInDate`). Copy in this one file, rebuild, restart.

0.99 **⚠️⚠️ PRIORITY FIX: hotel activation is now truly idempotent, and
   deactivation is now a real feature.** Previously `activateHotelById` had
   a comment literally saying `// Assuming Only Do it Once` — calling it a
   second time (e.g. after a future deactivate/reactivate cycle, or a direct
   API call bypassing the UI) would try to re-insert a full year of
   inventory per room, colliding with the DB's existing unique constraint
   on `(hotel_id, room_id, date)` and throwing an exception. Fixed at the
   root instead of just hiding a button in the UI:
   - **respository/InventoryRepository.java** — added `existsByRoom(Room)`
   - **service/HotelServiceImpl.java** — `activateHotelById` now loops the
     hotel's rooms and only initializes inventory for rooms that don't
     already have any (correctly handles: first activation, re-activation,
     AND rooms added while the hotel was inactive, which previously got no
     inventory at all until the next activate). Added `deactivateHotelById`
     — a plain flag flip with zero inventory side effects, trivially safe
     to call any number of times; existing bookings/inventory are untouched.
   - **service/HotelService.java** — added `deactivateHotelById` signature
   - **controller/HotelController.java** — new endpoint
     `PATCH /admin/hotels/{hotelId}/deactivate`
   Frontend now shows a real Deactivate button once a hotel is active
   (previously there was no way to deactivate at all).

0.95 **⚠️ Bug fix: editing a hotel was silently deactivating it.**
   `updateHotelById` did a blanket `modelMapper.map(hotelDTO, hotel)`. Since
   the edit form doesn't send `active` (not meant to be editable there), it
   deserialized as `null` and overwrote the hotel's real active status on
   every edit. Fixed in **service/HotelServiceImpl.java** by capturing the
   hotel's current `active` value before the mapping call and restoring it
   after — same pattern already used there for `amenities`/`photos`.
   Activation now only ever changes via the dedicated Activate endpoint, as
   intended.

0.9 **⚠️ Small fix for chunk 6 (admin panel): service/BookingServiceImpl.java**
   was updated again — `getAllBookingsInHotelById` now also sets `roomType`
   on each `BookingDTO`, same gap `getMyBookings` had before. Needed for the
   admin Bookings tab table. Re-copy this file even if you already have it.

0.75 **⚠️ New feature: self-service "Become a host" signup.** Adds a second
   signup path that creates the account directly with `HOTEL_MANAGER` role
   (no approval step). No security config changes needed — `/auth/**` was
   already anonymous-permitted, which covers the new path too.
   - **security/AuthService.java** — refactored `signUp` to share logic via
     a new private `createUser(dto, role)` helper; added `signUpAsHost(dto)`
     using the same helper with `Role.HOTEL_MANAGER`.
   - **controller/AuthController.java** — new endpoint `POST /auth/signup/host`

0.5 **⚠️ New feature: editing guests after they're added.** Previously
   `addGuests` only worked once (required status exactly `RESERVED`) and
   appended rather than replaced. Now there's a separate update path:
   - **service/BookingService.java** — added `updateGuests(...)` signature
   - **service/BookingServiceImpl.java** — implemented `updateGuests`:
     allowed only while status is `GUESTS_ADDED` or `PAYMENT_PENDING` (not
     yet `CONFIRMED`), clears the existing guest set and replaces it with
     the new list
   - **controller/HotelBookingController.java** — new endpoint:
     `PUT /bookings/{bookingId}/guests`
   (`BookingServiceImpl.java` overlaps with item 0 below — just copy the one
   version included here, it has both fixes.)

0. **⚠️ service/BookingServiceImpl.java** and **service/InventoryServiceImpl.java**
   were updated again since you last downloaded them — fixed a real
   overcharging bug: a stay from 13 Aug to 14 Aug (1 night) was being charged
   and inventory-reserved for **2** nights, because
   `ChronoUnit.DAYS.between(checkIn, checkOut) + 1` counts the checkout date
   itself as a chargeable night. Fixed in 4 places that all touch money or
   inventory counts:
   - `initializeBooking` — inventory lookup/lock, availability check, reserve
   - `capturePayment` — inventory lock/confirm when Stripe payment succeeds
   - `cancelBooking` — inventory release on cancellation
   - `getRoomPriceForDateRange` (the price preview endpoint) — kept
     consistent with the real calculation above
   All four now exclude the checkout date from the date range
   (`checkOutDate.minusDays(1)` as the effective last night), and the
   "nights" count no longer has `+ 1`. Re-copy these two files even if you
   already copied them in earlier.

1. **controller/HotelBrowseController.java**
   - `searchHotels`: removed `@RequestBody` so the DTO binds from query
     params instead (browsers can't send a body on GET).
   - Added `GET /hotels/rooms/{roomId}/price` — public room price preview.

2. **dto/BookingDTO.java**
   - Added `hotelId`, `hotelName`, `roomType` fields, used by My Bookings.

3. **dto/RoomPriceDTO.java** (new file)
   - `{ roomId, nights, roomsCount, totalPrice, available }` — response
     shape for the price preview endpoint.

4. **service/BookingServiceImpl.java**
   - `getMyBookings()`: now manually sets `hotelId`/`hotelName`/`roomType`
     on each `BookingDTO` (ModelMapper doesn't auto-map these).

5. **service/InventoryService.java**
   - Added `getRoomPriceForDateRange(...)` method signature.

6. **service/InventoryServiceImpl.java**
   - Implemented `getRoomPriceForDateRange(...)`, using the exact same
     inclusive date-range and `PricingService.calculateTotalPriceForOneRoom`
     logic as real booking, so the preview always matches checkout.
   - Added a `PricingService` field (constructor-injected via Lombok).

7. **respository/InventoryRepository.java**
   - Added `findAvailableInventoryNoLock(...)` — same availability query as
     `findAndLockAvailableInventory` but without the pessimistic lock, safe
     for a read-only price check.

## Changes NOT in this zip (you already applied these directly)

These were given as snippets/instructions earlier, not as file edits I made,
so there's nothing new to copy — just confirming what should already be in
your project:

- **`config/CorsConfig.java`** — a `CorsFilter` bean allowing
  `http://localhost:5173` with credentials.
- **`security/WebSecurityConfig.java`** — added
  `.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()` as the first
  rule, so CORS preflight requests aren't blocked by route-specific auth
  rules.
- **`application.properties`** — `frontend.url=http://localhost:5173`
  (was `https://localhost:8080`), used to build Stripe's post-checkout
  redirect URLs.
- **Stripe CLI command** — `stripe listen --forward-to
  localhost:8080/api/v1/webhooks/payment` (not a code change, just how you
  run the CLI).

If any of those four aren't actually in your project, let me know and I'll
regenerate them.

## After copying these files in

Rebuild before restarting (stale compiled classes have bitten us before):
```
mvn clean install
```

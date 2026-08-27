-- Opening stock for the demo catalogue.
--
-- Quantities are deliberately uneven so every stock state in the UI has real
-- data behind it rather than a hand-waved screenshot:
--   * one product at zero, so the out-of-stock state and the disabled
--     add-to-cart button can be demonstrated
--   * several at or below the reorder threshold of 5, so the admin dashboard's
--     low-stock tile is not an empty box
--   * the rest at healthy levels
--
-- Product ids match product-service exactly. They are derived from the SKU with
-- UUIDv5, which is how two independent databases agree on identifiers without
-- sharing a table or a foreign key.

INSERT INTO inventory_items (product_id, total_quantity, reserved_quantity, reorder_threshold, version, updated_at) VALUES
  ('343213de-c447-56c6-ac74-dd29fcff1fec', 42, 0, 5, 0, TIMESTAMP '2026-01-28 12:00:00'),
  ('06e48299-d406-5f29-bcee-728d902d65ea', 96, 0, 5, 0, TIMESTAMP '2026-01-28 12:00:00'),
  ('f4a53e54-9efb-570e-a0a9-59cc67112fce', 18, 0, 5, 0, TIMESTAMP '2026-01-28 12:00:00'),
  ('021f37b8-99e8-574a-8604-5c586e93b3e8', 9, 0, 5, 0, TIMESTAMP '2026-01-28 12:00:00'),
  ('2d4bc1ef-ba0d-514f-be5d-eac47ce54120', 24, 0, 5, 0, TIMESTAMP '2026-01-28 12:00:00'),
  ('4a74101a-ff9e-57ba-abd3-b01b84e58e96', 12, 0, 5, 0, TIMESTAMP '2026-01-28 12:00:00'),
  ('33c6f1f3-4e76-54ad-95e9-38692dba8f00', 61, 0, 5, 0, TIMESTAMP '2026-01-28 12:00:00'),
  ('6442b5d0-4b9a-5b67-be20-cb6eb3f3b81f', 134, 0, 5, 0, TIMESTAMP '2026-01-28 12:00:00'),
  ('03fdb501-0e02-5b63-bda8-a9e65a61dec4', 21, 0, 5, 0, TIMESTAMP '2026-01-28 12:00:00'),
  ('42427033-98d7-5020-a92f-49c6bdf9aa0c', 47, 0, 5, 0, TIMESTAMP '2026-01-28 12:00:00'),
  ('b41c22a0-cbb0-5c00-9aa3-0a7925db296d', 38, 0, 5, 0, TIMESTAMP '2026-01-28 12:00:00'),
  ('54dcfc85-e51c-5515-a8ce-7f11e2e34d30', 16, 0, 5, 0, TIMESTAMP '2026-01-28 12:00:00'),
  ('49e2b7fd-7d8f-534e-83ab-970e6b7e648d', 29, 0, 5, 0, TIMESTAMP '2026-01-28 12:00:00'),
  ('29dfc8bb-fbfe-5c2c-94c6-d7b007dfeab6', 0, 0, 5, 0, TIMESTAMP '2026-01-28 12:00:00'),
  ('c92604ae-a1c3-5a0e-9850-aa313fa0b2ae', 33, 0, 5, 0, TIMESTAMP '2026-01-28 12:00:00'),
  ('e4dd746f-c87a-53ab-8b65-fa3f642c041a', 88, 0, 5, 0, TIMESTAMP '2026-01-28 12:00:00'),
  ('132f0d76-19d4-5f94-9408-d8837e1106da', 71, 0, 5, 0, TIMESTAMP '2026-01-28 12:00:00'),
  ('b7e96173-0c4f-5771-95dd-a9129ef21d3e', 7, 0, 5, 0, TIMESTAMP '2026-01-28 12:00:00'),
  ('1049f1db-f249-551b-a0fe-838e88d5085f', 54, 0, 5, 0, TIMESTAMP '2026-01-28 12:00:00'),
  ('c794c12d-44ee-5378-913c-a2012877c992', 4, 0, 5, 0, TIMESTAMP '2026-01-28 12:00:00'),
  ('eeff95a6-fb55-5c76-ab0d-c9696b591527', 63, 0, 5, 0, TIMESTAMP '2026-01-28 12:00:00'),
  ('2a6528cc-98f5-5a4a-aebf-3d9e6b94a520', 8, 0, 5, 0, TIMESTAMP '2026-01-28 12:00:00'),
  ('5fae9de6-5fd8-5c89-b9bb-a40924a143d1', 15, 0, 5, 0, TIMESTAMP '2026-01-28 12:00:00'),
  ('a9873b9f-ead9-57d9-b4e4-07b3be71de45', 77, 0, 5, 0, TIMESTAMP '2026-01-28 12:00:00'),
  ('7a42ea07-0de4-583e-b059-a5e2349fd3c9', 3, 0, 5, 0, TIMESTAMP '2026-01-28 12:00:00');

-- The opening balance is itself a ledger entry, so the history is complete from
-- the first row rather than starting mid-story.
INSERT INTO stock_transactions (id, product_id, type, quantity, resulting_total, resulting_reserved, reference_id, occurred_at) VALUES
  ('ceb10e3a-9275-55b9-825b-7fd725b48224', '343213de-c447-56c6-ac74-dd29fcff1fec', 'INITIAL', 42, 42, 0, NULL, TIMESTAMP '2026-01-28 12:00:00'),
  ('5b1784f9-a97b-51d2-addb-becc9c809e14', '06e48299-d406-5f29-bcee-728d902d65ea', 'INITIAL', 96, 96, 0, NULL, TIMESTAMP '2026-01-28 12:00:00'),
  ('c67a75de-b98f-5775-9d19-065026553611', 'f4a53e54-9efb-570e-a0a9-59cc67112fce', 'INITIAL', 18, 18, 0, NULL, TIMESTAMP '2026-01-28 12:00:00'),
  ('7f33ae2e-f4b3-51f7-b581-d4558c8c2234', '021f37b8-99e8-574a-8604-5c586e93b3e8', 'INITIAL', 9, 9, 0, NULL, TIMESTAMP '2026-01-28 12:00:00'),
  ('e28864fd-4b43-540d-93e5-8cf3e54de5fa', '2d4bc1ef-ba0d-514f-be5d-eac47ce54120', 'INITIAL', 24, 24, 0, NULL, TIMESTAMP '2026-01-28 12:00:00'),
  ('0cd60783-1153-53d9-a5c3-48229f3c1c5a', '4a74101a-ff9e-57ba-abd3-b01b84e58e96', 'INITIAL', 12, 12, 0, NULL, TIMESTAMP '2026-01-28 12:00:00'),
  ('658558cf-471e-589d-9c9e-771db763a850', '33c6f1f3-4e76-54ad-95e9-38692dba8f00', 'INITIAL', 61, 61, 0, NULL, TIMESTAMP '2026-01-28 12:00:00'),
  ('89532038-7b1b-57f8-ac61-1d84c3be4ca0', '6442b5d0-4b9a-5b67-be20-cb6eb3f3b81f', 'INITIAL', 134, 134, 0, NULL, TIMESTAMP '2026-01-28 12:00:00'),
  ('1fdbe6f3-c347-5b84-b54c-61a09c99b07e', '03fdb501-0e02-5b63-bda8-a9e65a61dec4', 'INITIAL', 21, 21, 0, NULL, TIMESTAMP '2026-01-28 12:00:00'),
  ('48e922b5-61dd-5df5-9418-e8efd6eeac8c', '42427033-98d7-5020-a92f-49c6bdf9aa0c', 'INITIAL', 47, 47, 0, NULL, TIMESTAMP '2026-01-28 12:00:00'),
  ('e80fe00c-e070-5653-bd47-40924f7e8915', 'b41c22a0-cbb0-5c00-9aa3-0a7925db296d', 'INITIAL', 38, 38, 0, NULL, TIMESTAMP '2026-01-28 12:00:00'),
  ('03967e23-3a2a-59f3-bd25-5f67b8179271', '54dcfc85-e51c-5515-a8ce-7f11e2e34d30', 'INITIAL', 16, 16, 0, NULL, TIMESTAMP '2026-01-28 12:00:00'),
  ('7fe2ea23-4d16-57ae-9530-cd4330d26828', '49e2b7fd-7d8f-534e-83ab-970e6b7e648d', 'INITIAL', 29, 29, 0, NULL, TIMESTAMP '2026-01-28 12:00:00'),
  ('01f7fff9-0b09-5237-9989-f4c8b8d08c3c', '29dfc8bb-fbfe-5c2c-94c6-d7b007dfeab6', 'INITIAL', 0, 0, 0, NULL, TIMESTAMP '2026-01-28 12:00:00'),
  ('732af3fa-23ec-5a30-bf84-4196c20170fd', 'c92604ae-a1c3-5a0e-9850-aa313fa0b2ae', 'INITIAL', 33, 33, 0, NULL, TIMESTAMP '2026-01-28 12:00:00'),
  ('3aece39a-5630-5673-9a87-6a02ece976d2', 'e4dd746f-c87a-53ab-8b65-fa3f642c041a', 'INITIAL', 88, 88, 0, NULL, TIMESTAMP '2026-01-28 12:00:00'),
  ('eca66bbb-d0a7-56a8-97ac-6f792eae556f', '132f0d76-19d4-5f94-9408-d8837e1106da', 'INITIAL', 71, 71, 0, NULL, TIMESTAMP '2026-01-28 12:00:00'),
  ('4ccfccbe-afa7-5bf4-afc1-eb2c410a6582', 'b7e96173-0c4f-5771-95dd-a9129ef21d3e', 'INITIAL', 7, 7, 0, NULL, TIMESTAMP '2026-01-28 12:00:00'),
  ('29f55e02-031d-5cf0-b08e-77331baffe88', '1049f1db-f249-551b-a0fe-838e88d5085f', 'INITIAL', 54, 54, 0, NULL, TIMESTAMP '2026-01-28 12:00:00'),
  ('e04ceaaa-56df-5344-a6d7-11ffb000ce7b', 'c794c12d-44ee-5378-913c-a2012877c992', 'INITIAL', 4, 4, 0, NULL, TIMESTAMP '2026-01-28 12:00:00'),
  ('0e126446-a54a-5601-9b95-80c28a6deba1', 'eeff95a6-fb55-5c76-ab0d-c9696b591527', 'INITIAL', 63, 63, 0, NULL, TIMESTAMP '2026-01-28 12:00:00'),
  ('f14a5260-cd9e-5c91-b28b-316c0d7efcad', '2a6528cc-98f5-5a4a-aebf-3d9e6b94a520', 'INITIAL', 8, 8, 0, NULL, TIMESTAMP '2026-01-28 12:00:00'),
  ('69889c44-68ce-5d9a-8990-ba4428405737', '5fae9de6-5fd8-5c89-b9bb-a40924a143d1', 'INITIAL', 15, 15, 0, NULL, TIMESTAMP '2026-01-28 12:00:00'),
  ('b741fa59-23d1-5222-9d18-0488d38d1839', 'a9873b9f-ead9-57d9-b4e4-07b3be71de45', 'INITIAL', 77, 77, 0, NULL, TIMESTAMP '2026-01-28 12:00:00'),
  ('58bab946-19ca-5830-8e1c-0b88f43a1ba1', '7a42ea07-0de4-583e-b059-a5e2349fd3c9', 'INITIAL', 3, 3, 0, NULL, TIMESTAMP '2026-01-28 12:00:00');

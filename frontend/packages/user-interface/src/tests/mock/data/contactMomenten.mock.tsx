/*
 * Copyright 2015-2026 Den Haag, Ritense, the Netherlands.
 *
 * Licensed under EUPL, Version 1.2 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import { QUERY_GET_USER_KLANT_CONTACTEN } from "@nl-portal/nl-portal-api";

export const getObjectContactMomenten = {
  request: {
    query: QUERY_GET_USER_KLANT_CONTACTEN,
    variables: {
      identificatorType: "ZAAK",
      identificatorId: "82cb13cf-d2f9-4e3e-ac07-751373035ecb",
    },
  },
  result: {
    data: {
      getUserKlantContacten: [
        {
          uuid: "f6b89308-7c91-4ca3-a280-4dc08a69de7c",
          inhoud:
            "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Cras ut aliquam velit.",
          kanaal: "Telefoon",
          onderwerp: "Klacht",
          plaatsgevondenOp: "2025-01-06T11:02:24Z",
          __typename: "OpenKlant2Klantcontact",
        },
        {
          uuid: "482a8529-0ebd-4424-83a9-b9f88335673d",
          inhoud:
            "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Cras ut aliquam velit.",
          kanaal: "E-mail",
          onderwerp: "Vraag over vergunningsaanvraag",
          plaatsgevondenOp: "2025-03-06T11:02:24Z",
          __typename: "OpenKlant2Klantcontact",
        },
        {
          uuid: "482a8529-0ebd-4424-83a9-b9f88335673d",
          inhoud:
            "Lorem ipsum dolor sit amet, consectetuer adipiscing elit. Aenean commodo ligula eget dolor. Aenean massa.\nCum sociis natoque penatibus et magnis dis parturient montes, nascetur ridiculus mus.\nDonec quam felis, ultricies nec, pellentesque eu, pretium quis, sem. Nulla consequat massa quis enim. Donec pede justo, fringilla vel, aliquet nec, vulputate eget, arcu.",
          kanaal: "SMS",
          onderwerp: "Dit is een sms",
          plaatsgevondenOp: "2025-03-12T10:00:12Z",
          __typename: "OpenKlant2Klantcontact",
        },
      ],
    },
  },
};

export const getFailingObjectContactMomenten = {
  request: {
    query: QUERY_GET_USER_KLANT_CONTACTEN,
    variables: {
      identificatorType: "ZAAK",
      identificatorId: "82cb13cf-d2f9-4e3e-ac07-751373035ecb",
    },
  },
  error: new Error("Network error"),
};

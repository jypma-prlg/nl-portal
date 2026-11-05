export type Maybe<T> = T | null;
export type InputMaybe<T> = Maybe<T>;
/** All built-in and custom scalars, mapped to their actual values */
export type Scalars = {
  ID: { input: string; output: string; }
  String: { input: string; output: string; }
  Boolean: { input: boolean; output: boolean; }
  Int: { input: number; output: number; }
  Float: { input: number; output: number; }
  BigDecimal: { input: number; output: number; }
  BigInteger: { input: number; output: number; }
  Date: { input: string; output: string; }
  DateTime: { input: string; output: string; }
  JSON: { input: any; output: any; }
  LocalDateTime: { input: string; output: string; }
  LocalTime: { input: string; output: string; }
  Locale: { input: string; output: string; }
  Long: { input: number; output: number; }
  PositiveFloat: { input: number; output: number; }
  UUID: { input: string; output: string; }
  ZonedDateTime: { input: string; output: string; }
};

export type ActiesPage = {
  __typename?: 'ActiesPage';
  /**  The elements on this page */
  content: Array<OpenProductActie>;
  /**  The requested page number */
  number: Scalars['Int']['output'];
  /**  The number of elements on this page */
  numberOfElements: Scalars['Int']['output'];
  /**  The requested page size */
  size: Scalars['Int']['output'];
  /**  The total number of elements */
  totalElements: Scalars['Int']['output'];
  /**  The total number of available pages */
  totalPages: Scalars['Int']['output'];
};

export type Adres = {
  __typename?: 'Adres';
  huisnummer?: Maybe<Scalars['Int']['output']>;
  indAfgeschermd: Scalars['String']['output'];
  land: Scalars['String']['output'];
  plaats: Scalars['String']['output'];
  postbusnummer?: Maybe<Scalars['Int']['output']>;
  postcode: Scalars['String']['output'];
  straatnaam?: Maybe<Scalars['String']['output']>;
  type: Scalars['String']['output'];
  volledigAdres: Scalars['String']['output'];
};

export type Bericht = {
  __typename?: 'Bericht';
  berichtTekst: Scalars['String']['output'];
  berichtType: BerichtType;
  bijlages: Array<Scalars['String']['output']>;
  documenten: Array<Document>;
  einddatumHandelingstermijn: Scalars['ZonedDateTime']['output'];
  geopend: Scalars['Boolean']['output'];
  handelingsperspectief: BerichtHandelingsperspectief;
  id?: Maybe<Scalars['UUID']['output']>;
  identificatie: BerichtIdentificatie;
  onderwerp: Scalars['String']['output'];
  publicatiedatum: Scalars['ZonedDateTime']['output'];
  referentie?: Maybe<Scalars['String']['output']>;
};

export enum BerichtHandelingsperspectief {
  Betalen = 'BETALEN',
  InformatieOntvangen = 'INFORMATIE_ONTVANGEN',
  InformatieVerstrekken = 'INFORMATIE_VERSTREKKEN'
}

export type BerichtIdentificatie = {
  __typename?: 'BerichtIdentificatie';
  type: Scalars['String']['output'];
  value: Scalars['String']['output'];
};

export enum BerichtType {
  Betaalverzoek = 'BETAALVERZOEK',
  Notificatie = 'NOTIFICATIE',
  Uitnodiging = 'UITNODIGING',
  Verzoek = 'VERZOEK'
}

export type BerichtenPage = {
  __typename?: 'BerichtenPage';
  /**  The elements on this page */
  content: Array<Bericht>;
  /**  The requested page number */
  number: Scalars['Int']['output'];
  /**  The number of elements on this page */
  numberOfElements: Scalars['Int']['output'];
  /**  The requested page size */
  size: Scalars['Int']['output'];
  /**  The total number of elements */
  totalElements: Scalars['Int']['output'];
  /**  The total number of available pages */
  totalPages: Scalars['Int']['output'];
};

export type Besluit = {
  __typename?: 'Besluit';
  audittrails: Array<BesluitAuditTrail>;
  besluittype: BesluitType;
  bestuursorgaan?: Maybe<Scalars['String']['output']>;
  datum: Scalars['Date']['output'];
  documenten: Array<Document>;
  identificatie: Scalars['String']['output'];
  ingangsdatum: Scalars['Date']['output'];
  publicatiedatum?: Maybe<Scalars['Date']['output']>;
  toelichting?: Maybe<Scalars['String']['output']>;
  uiterlijkeReactiedatum?: Maybe<Scalars['Date']['output']>;
  url: Scalars['String']['output'];
  verantwoordelijkeOrganisatie: Scalars['String']['output'];
  vervaldatum?: Maybe<Scalars['Date']['output']>;
  vervalreden: Scalars['String']['output'];
  vervalredenWeergave: Scalars['String']['output'];
  verzenddatum?: Maybe<Scalars['Date']['output']>;
  zaak?: Maybe<Scalars['String']['output']>;
};

export type BesluitAuditTrail = {
  __typename?: 'BesluitAuditTrail';
  aanmaakdatum?: Maybe<Scalars['ZonedDateTime']['output']>;
  actie: Scalars['String']['output'];
  actieWeergave?: Maybe<Scalars['String']['output']>;
  applicatieId?: Maybe<Scalars['String']['output']>;
  applicatieWeergave?: Maybe<Scalars['String']['output']>;
  bron: Scalars['String']['output'];
  gebruikersId?: Maybe<Scalars['String']['output']>;
  gebruikersWeergave?: Maybe<Scalars['String']['output']>;
  hoofdObject: Scalars['String']['output'];
  resource: Scalars['String']['output'];
  resourceUrl: Scalars['String']['output'];
  resourceWeergave: Scalars['String']['output'];
  resultaat: Scalars['Int']['output'];
  toelichting?: Maybe<Scalars['String']['output']>;
  uuid: Scalars['UUID']['output'];
  wijzigingen: BesluitAuditTrailWijzigingen;
};

export type BesluitAuditTrailWijzigingen = {
  __typename?: 'BesluitAuditTrailWijzigingen';
  nieuw?: Maybe<Scalars['JSON']['output']>;
  oud?: Maybe<Scalars['JSON']['output']>;
};

export type BesluitDocument = {
  __typename?: 'BesluitDocument';
  besluit: Scalars['String']['output'];
  informatieobject: Scalars['String']['output'];
  url: Scalars['String']['output'];
};

export type BesluitPage = {
  __typename?: 'BesluitPage';
  /**  The elements on this page */
  content: Array<Besluit>;
  /**  The requested page number */
  number: Scalars['Int']['output'];
  /**  The number of elements on this page */
  numberOfElements: Scalars['Int']['output'];
  /**  The requested page size */
  size: Scalars['Int']['output'];
  /**  The total number of elements */
  totalElements: Scalars['Int']['output'];
  /**  The total number of available pages */
  totalPages: Scalars['Int']['output'];
};

export type BesluitType = {
  __typename?: 'BesluitType';
  besluitcategorie: Scalars['String']['output'];
  omschrijving?: Maybe<Scalars['String']['output']>;
  omschrijvingGeneriek?: Maybe<Scalars['String']['output']>;
  publicatieIndicatie: Scalars['Boolean']['output'];
  publicatietekst?: Maybe<Scalars['String']['output']>;
  publicatietermijn?: Maybe<Scalars['String']['output']>;
  reactietermijn?: Maybe<Scalars['String']['output']>;
  toelichting?: Maybe<Scalars['String']['output']>;
  url: Scalars['String']['output'];
  zaaktypen: Array<Scalars['String']['output']>;
};

export type BestandenPage = {
  __typename?: 'BestandenPage';
  /**  The elements on this page */
  content: Array<OpenProductBestand>;
  /**  The requested page number */
  number: Scalars['Int']['output'];
  /**  The number of elements on this page */
  numberOfElements: Scalars['Int']['output'];
  /**  The requested page size */
  size: Scalars['Int']['output'];
  /**  The total number of elements */
  totalElements: Scalars['Int']['output'];
  /**  The total number of available pages */
  totalPages: Scalars['Int']['output'];
};

export type Betrokkene = {
  __typename?: 'Betrokkene';
  bezoekadres?: Maybe<OpenKlant2Adres>;
  contactnaam?: Maybe<Contactnaam>;
  correspondentieadres?: Maybe<OpenKlant2Adres>;
  digitaleAdressen: Array<OpenKlant2ForeignKey>;
  hadKlantcontact: OpenKlant2ForeignKey;
  initiator: Scalars['Boolean']['output'];
  organisatienaam: Scalars['String']['output'];
  rol: Scalars['String']['output'];
  url: Scalars['String']['output'];
  uuid: Scalars['String']['output'];
  volledigeNaam: Scalars['String']['output'];
  wasPartij?: Maybe<OpenKlant2ForeignKey>;
};

export type Brp2Adres = {
  __typename?: 'Brp2Adres';
  aanduidingBijHuisnummer?: Maybe<BrpCodeOmschrijving>;
  huisletter?: Maybe<Scalars['String']['output']>;
  huisnummer?: Maybe<Scalars['Int']['output']>;
  huisnummertoevoeging?: Maybe<Scalars['String']['output']>;
  inOnderzoek?: Maybe<Brp2AdresInOnderzoek>;
  korteStraatnaam?: Maybe<Scalars['String']['output']>;
  officieleStraatnaam?: Maybe<Scalars['String']['output']>;
  postcode?: Maybe<Scalars['String']['output']>;
  woonplaats?: Maybe<Scalars['String']['output']>;
};

export type Brp2AdresInOnderzoek = {
  __typename?: 'Brp2AdresInOnderzoek';
  aanduidingBijHuisnummer?: Maybe<Scalars['Boolean']['output']>;
  huisletter?: Maybe<Scalars['Boolean']['output']>;
  huisnummer?: Maybe<Scalars['Boolean']['output']>;
  huisnummertoevoeging?: Maybe<Scalars['Boolean']['output']>;
  korteStraatnaam?: Maybe<Scalars['Boolean']['output']>;
  officieleStraatnaam?: Maybe<Scalars['Boolean']['output']>;
  postcode?: Maybe<Scalars['Boolean']['output']>;
  woonplaats?: Maybe<Scalars['Boolean']['output']>;
};

export type Brp2NationaliteitInOnderzoek = {
  __typename?: 'Brp2NationaliteitInOnderzoek';
  datumIngangGeldigheid?: Maybe<Scalars['Boolean']['output']>;
  datumIngangOnderzoek?: Maybe<BrpDatum>;
  nationaliteit?: Maybe<Scalars['Boolean']['output']>;
  redenOpname?: Maybe<Scalars['Boolean']['output']>;
  type?: Maybe<Scalars['Boolean']['output']>;
};

export type BrpAdellijkeTitelPredicaat = {
  __typename?: 'BrpAdellijkeTitelPredicaat';
  code?: Maybe<Scalars['String']['output']>;
  omschrijving?: Maybe<Scalars['String']['output']>;
  soort?: Maybe<Scalars['String']['output']>;
};

export type BrpAdressering = {
  __typename?: 'BrpAdressering';
  aanhef?: Maybe<Scalars['String']['output']>;
  aanschrijfwijze?: Maybe<BrpAdresseringAanschrijfwijze>;
  adresregel1?: Maybe<Scalars['String']['output']>;
  adresregel2?: Maybe<Scalars['String']['output']>;
  adresregel3?: Maybe<Scalars['String']['output']>;
  gebruikInLopendeTekst?: Maybe<Scalars['String']['output']>;
  indicatieVastgesteldVerblijftNietOpAdres?: Maybe<Scalars['Boolean']['output']>;
  land?: Maybe<BrpCodeOmschrijving>;
};

export type BrpAdresseringAanschrijfwijze = {
  __typename?: 'BrpAdresseringAanschrijfwijze';
  aanspreekvorm?: Maybe<Scalars['String']['output']>;
  naam?: Maybe<Scalars['String']['output']>;
};

export type BrpCodeOmschrijving = {
  __typename?: 'BrpCodeOmschrijving';
  code?: Maybe<Scalars['String']['output']>;
  omschrijving?: Maybe<Scalars['String']['output']>;
};

export type BrpDatum = {
  __typename?: 'BrpDatum';
  datum?: Maybe<Scalars['Date']['output']>;
  langFormaat?: Maybe<Scalars['String']['output']>;
  type?: Maybe<Scalars['String']['output']>;
};

export type BrpDatumLandPlaats = {
  __typename?: 'BrpDatumLandPlaats';
  datum?: Maybe<BrpDatum>;
  inOnderzoek?: Maybe<BrpDatumLandPlaatsInOnderzoek>;
  land?: Maybe<BrpCodeOmschrijving>;
  plaats?: Maybe<BrpCodeOmschrijving>;
};

export type BrpDatumLandPlaatsInOnderzoek = {
  __typename?: 'BrpDatumLandPlaatsInOnderzoek';
  datum?: Maybe<Scalars['Boolean']['output']>;
  datumIngangOnderzoek?: Maybe<BrpDatum>;
  land?: Maybe<Scalars['Boolean']['output']>;
  plaats?: Maybe<Scalars['Boolean']['output']>;
};

export type BrpEuropeesKiesrecht = {
  __typename?: 'BrpEuropeesKiesrecht';
  aanduiding?: Maybe<BrpCodeOmschrijving>;
  einddatumUitsluiting?: Maybe<BrpDatum>;
};

export type BrpGezag = {
  __typename?: 'BrpGezag';
  derde?: Maybe<BrpGezagDerde>;
  derden?: Maybe<Array<BrpGezagDerde>>;
  minderjarige?: Maybe<BrpGezagMinderjarige>;
  ouder?: Maybe<BrpGezagOuder>;
  ouders?: Maybe<Array<BrpGezagOuder>>;
  type?: Maybe<Scalars['String']['output']>;
};

export type BrpGezagDerde = {
  __typename?: 'BrpGezagDerde';
  burgerservicenummer?: Maybe<Scalars['String']['output']>;
  naam?: Maybe<BrpGezagNaam>;
  type?: Maybe<Scalars['String']['output']>;
};

export type BrpGezagMinderjarige = {
  __typename?: 'BrpGezagMinderjarige';
  burgerservicenummer?: Maybe<Scalars['String']['output']>;
  leeftijd?: Maybe<Scalars['Int']['output']>;
  naam?: Maybe<BrpGezagNaam>;
};

export type BrpGezagNaam = {
  __typename?: 'BrpGezagNaam';
  volledigeNaam?: Maybe<Scalars['String']['output']>;
};

export type BrpGezagOuder = {
  __typename?: 'BrpGezagOuder';
  burgerservicenummer?: Maybe<Scalars['String']['output']>;
  naam?: Maybe<BrpGezagNaam>;
};

export type BrpImigratie = {
  __typename?: 'BrpImigratie';
  datumVestigingInNederland?: Maybe<BrpDatum>;
  inOnderzoek?: Maybe<BrpImigratieInOnderzoek>;
  indicatieVestigingVanuitBuitenland?: Maybe<Scalars['Boolean']['output']>;
  landVanwaarIngeschreven?: Maybe<BrpCodeOmschrijving>;
  vanuitVerblijfplaatsOnbekend?: Maybe<Scalars['Boolean']['output']>;
};

export type BrpImigratieInOnderzoek = {
  __typename?: 'BrpImigratieInOnderzoek';
  datumIngangOnderzoek?: Maybe<BrpDatum>;
  datumVestigingInNederland?: Maybe<Scalars['Boolean']['output']>;
  indicatieVestigingVanuitBuitenland?: Maybe<Scalars['Boolean']['output']>;
  landVanwaarIngeschreven?: Maybe<Scalars['Boolean']['output']>;
  vanuitVerblijfplaatsOnbekend?: Maybe<Scalars['Boolean']['output']>;
};

export type BrpInOnderzoek = {
  __typename?: 'BrpInOnderzoek';
  burgerservicenummer?: Maybe<Scalars['Boolean']['output']>;
  datumIngangOnderzoekGemeente?: Maybe<BrpDatum>;
  datumIngangOnderzoekGezag?: Maybe<BrpDatum>;
  datumIngangOnderzoekPersoon?: Maybe<BrpDatum>;
  datumInschrijvingInGemeente?: Maybe<Scalars['Boolean']['output']>;
  gemeenteVanInschrijving?: Maybe<Scalars['Boolean']['output']>;
  geslacht?: Maybe<Scalars['Boolean']['output']>;
  indicatieCurateleRegister?: Maybe<Scalars['Boolean']['output']>;
  /**
   * deprecated(
   *  reason: "This value is deprecated and should be removed."
   * )
   */
  indicatieGezagMinderjarige?: Maybe<Scalars['Boolean']['output']>;
  leeftijd?: Maybe<Scalars['Boolean']['output']>;
};

export type BrpKind = {
  __typename?: 'BrpKind';
  burgerservicenummer?: Maybe<Scalars['String']['output']>;
  geboorte?: Maybe<BrpDatumLandPlaats>;
  inOnderzoek?: Maybe<BrpKindInOnderzoek>;
  naam?: Maybe<BrpNaam>;
};

export type BrpKindInOnderzoek = {
  __typename?: 'BrpKindInOnderzoek';
  burgerservicenummer?: Maybe<Scalars['Boolean']['output']>;
  datumIngangOnderzoek?: Maybe<BrpDatum>;
};

export type BrpNaam = {
  __typename?: 'BrpNaam';
  aanduidingNaamgebruik?: Maybe<BrpCodeOmschrijving>;
  adellijkeTitelPredicaat?: Maybe<BrpAdellijkeTitelPredicaat>;
  geslachtsnaam?: Maybe<Scalars['String']['output']>;
  inOnderzoek?: Maybe<BrpNaamInOnderzoek>;
  lastName: Scalars['String']['output'];
  officialLastName?: Maybe<Scalars['String']['output']>;
  volledigeNaam?: Maybe<Scalars['String']['output']>;
  voorletters?: Maybe<Scalars['String']['output']>;
  voornamen?: Maybe<Scalars['String']['output']>;
  voorvoegsel?: Maybe<Scalars['String']['output']>;
};

export type BrpNaamInOnderzoek = {
  __typename?: 'BrpNaamInOnderzoek';
  aanduidingNaamgebruik?: Maybe<Scalars['Boolean']['output']>;
  adellijkeTitelPredicaat?: Maybe<Scalars['Boolean']['output']>;
  datumIngangOnderzoek?: Maybe<BrpDatum>;
  geslachtsnaam?: Maybe<Scalars['Boolean']['output']>;
  volledigeNaam?: Maybe<Scalars['Boolean']['output']>;
  voorletters?: Maybe<Scalars['Boolean']['output']>;
  voornamen?: Maybe<Scalars['Boolean']['output']>;
  voorvoegsel?: Maybe<Scalars['Boolean']['output']>;
};

export type BrpNationaliteit = {
  __typename?: 'BrpNationaliteit';
  datumIngangGeldigheid?: Maybe<BrpCodeOmschrijving>;
  inOnderzoek?: Maybe<Brp2NationaliteitInOnderzoek>;
  nationaliteit?: Maybe<BrpCodeOmschrijving>;
  redenOpname?: Maybe<BrpCodeOmschrijving>;
  type?: Maybe<Scalars['String']['output']>;
};

export type BrpOuder = {
  __typename?: 'BrpOuder';
  burgerservicenummer?: Maybe<Scalars['String']['output']>;
  datumIngangFamilierechtelijkeBetrekking?: Maybe<BrpDatum>;
  geboorte?: Maybe<BrpDatumLandPlaats>;
  geslacht?: Maybe<BrpCodeOmschrijving>;
  inOnderzoek?: Maybe<BrpOuderInOnderzoek>;
  naam?: Maybe<BrpNaam>;
  ouderAanduiding?: Maybe<Scalars['String']['output']>;
};

export type BrpOuderInOnderzoek = {
  __typename?: 'BrpOuderInOnderzoek';
  burgerservicenummer?: Maybe<Scalars['Boolean']['output']>;
  datumIngangFamilierechtelijkeBetrekking?: Maybe<Scalars['Boolean']['output']>;
  datumIngangOnderzoek?: Maybe<BrpDatum>;
  geslacht?: Maybe<Scalars['Boolean']['output']>;
};

export type BrpPartner = {
  __typename?: 'BrpPartner';
  aangaanHuwelijkPartnerschap?: Maybe<BrpPartnerHuwelijkAangaan>;
  burgerservicenummer?: Maybe<Scalars['String']['output']>;
  geboorte?: Maybe<BrpDatumLandPlaats>;
  geslacht?: Maybe<BrpCodeOmschrijving>;
  inOnderzoek?: Maybe<BrpPartnerInOnderzoek>;
  naam?: Maybe<BrpNaam>;
  ontbindingHuwelijkPartnerschap?: Maybe<BrpPartnerHuwelijkOntbinding>;
  soortVerbintenis?: Maybe<BrpCodeOmschrijving>;
};

export type BrpPartnerHuwelijkAangaan = {
  __typename?: 'BrpPartnerHuwelijkAangaan';
  datum?: Maybe<BrpDatum>;
  inOnderzoek?: Maybe<BrpPartnerHuwelijkAangaanInOnderzoek>;
  land?: Maybe<BrpCodeOmschrijving>;
  plaats?: Maybe<BrpCodeOmschrijving>;
  soortVerbintenis?: Maybe<BrpCodeOmschrijving>;
};

export type BrpPartnerHuwelijkAangaanInOnderzoek = {
  __typename?: 'BrpPartnerHuwelijkAangaanInOnderzoek';
  datum?: Maybe<Scalars['Boolean']['output']>;
  datumIngangOnderzoek?: Maybe<BrpDatum>;
  land?: Maybe<Scalars['Boolean']['output']>;
  plaats?: Maybe<Scalars['Boolean']['output']>;
  soortVerbintenis?: Maybe<Scalars['Boolean']['output']>;
};

export type BrpPartnerHuwelijkOntbinding = {
  __typename?: 'BrpPartnerHuwelijkOntbinding';
  datum?: Maybe<BrpDatum>;
  inOnderzoek?: Maybe<BrpPartnerHuwelijkOntbindingInOnderzoek>;
};

export type BrpPartnerHuwelijkOntbindingInOnderzoek = {
  __typename?: 'BrpPartnerHuwelijkOntbindingInOnderzoek';
  datum?: Maybe<Scalars['Boolean']['output']>;
  datumIngangOnderzoek?: Maybe<BrpDatum>;
};

export type BrpPartnerInOnderzoek = {
  __typename?: 'BrpPartnerInOnderzoek';
  burgerservicenummer?: Maybe<Scalars['Boolean']['output']>;
  datumIngangOnderzoek?: Maybe<BrpDatum>;
  geslacht?: Maybe<Scalars['Boolean']['output']>;
  soortVerbintenis?: Maybe<Scalars['Boolean']['output']>;
};

export type BrpPersoon = {
  __typename?: 'BrpPersoon';
  adressering?: Maybe<BrpAdressering>;
  bewonersAantal?: Maybe<Scalars['Int']['output']>;
  burgerservicenummer?: Maybe<Scalars['String']['output']>;
  datumEersteInschrijvingGBA?: Maybe<BrpDatum>;
  datumInschrijvingInGemeente?: Maybe<BrpDatum>;
  europeesKiesrecht?: Maybe<BrpEuropeesKiesrecht>;
  geboorte?: Maybe<BrpDatumLandPlaats>;
  geheimhoudingPersoonsgegevens?: Maybe<Scalars['Boolean']['output']>;
  gemeenteVanInschrijving?: Maybe<BrpCodeOmschrijving>;
  geslacht?: Maybe<BrpCodeOmschrijving>;
  gezag?: Maybe<Array<BrpGezag>>;
  imigratie?: Maybe<BrpImigratie>;
  inOnderzoek?: Maybe<BrpInOnderzoek>;
  indicatieCurateleRegister?: Maybe<Scalars['Boolean']['output']>;
  /**
   * deprecated(
   *  reason: "This value is deprecated and should be removed."
   * )
   */
  indicatieGezagMinderjarige?: Maybe<BrpCodeOmschrijving>;
  kinderen?: Maybe<Array<BrpKind>>;
  leeftijd?: Maybe<Scalars['Int']['output']>;
  naam?: Maybe<BrpNaam>;
  nationaliteiten?: Maybe<Array<BrpNationaliteit>>;
  ouders?: Maybe<Array<BrpOuder>>;
  overlijden?: Maybe<BrpDatumLandPlaats>;
  partners?: Maybe<Array<BrpPartner>>;
  rni?: Maybe<Array<BrpPersoonRni>>;
  uitsluitingKiesrecht?: Maybe<BrpUitsluitingKiesrecht>;
  verblijfplaats?: Maybe<BrpVerblijfplaats>;
  verblijfstitel?: Maybe<BrpVerblijfsTitel>;
  verificatie?: Maybe<BrpPersoonVerificatie>;
};

export type BrpPersoonRni = {
  __typename?: 'BrpPersoonRni';
  categorie?: Maybe<Scalars['String']['output']>;
  deelnemer?: Maybe<BrpCodeOmschrijving>;
  omschrijvingVerdrag?: Maybe<Scalars['String']['output']>;
};

export type BrpPersoonVerificatie = {
  __typename?: 'BrpPersoonVerificatie';
  datum?: Maybe<BrpDatum>;
  omschrijving?: Maybe<Scalars['String']['output']>;
};

export type BrpUitsluitingKiesrecht = {
  __typename?: 'BrpUitsluitingKiesrecht';
  einddatum?: Maybe<BrpDatum>;
  uitgeslotenVanKiesrecht?: Maybe<Scalars['Boolean']['output']>;
};

export type BrpVerblijfplaats = {
  __typename?: 'BrpVerblijfplaats';
  adresseerbaarObjectIdentificatie?: Maybe<Scalars['String']['output']>;
  datumVan?: Maybe<BrpDatum>;
  functieAdres?: Maybe<BrpCodeOmschrijving>;
  inOnderzoek?: Maybe<BrpVerblijfplaatsInOnderzoek>;
  indicatieVastgesteldVerblijftNietOpAdres?: Maybe<Scalars['Boolean']['output']>;
  nummeraanduidingIdentificatie?: Maybe<Scalars['String']['output']>;
  type?: Maybe<Scalars['String']['output']>;
  verblijfadres?: Maybe<Brp2Adres>;
};

export type BrpVerblijfplaatsInOnderzoek = {
  __typename?: 'BrpVerblijfplaatsInOnderzoek';
  adresseerbaarObjectIdentificatie?: Maybe<Scalars['Boolean']['output']>;
  datumIngangOnderzoek?: Maybe<BrpDatum>;
  datumVan?: Maybe<Scalars['Boolean']['output']>;
  functieAdres?: Maybe<Scalars['Boolean']['output']>;
  indicatieVastgesteldVerblijftNietOpAdres?: Maybe<Scalars['Boolean']['output']>;
  nummeraanduidingIdentificatie?: Maybe<Scalars['Boolean']['output']>;
  type?: Maybe<Scalars['Boolean']['output']>;
  verblijfplaats?: Maybe<Scalars['Boolean']['output']>;
};

export type BrpVerblijfsTitel = {
  __typename?: 'BrpVerblijfsTitel';
  aanduiding?: Maybe<BrpCodeOmschrijving>;
  datumEinde?: Maybe<BrpDatum>;
  datumIngang?: Maybe<BrpDatum>;
  inOnderzoek?: Maybe<BrpVerblijfsTitelInOnderzoek>;
};

export type BrpVerblijfsTitelInOnderzoek = {
  __typename?: 'BrpVerblijfsTitelInOnderzoek';
  aanduiding?: Maybe<Scalars['Boolean']['output']>;
  datumEinde?: Maybe<Scalars['Boolean']['output']>;
  datumIngang?: Maybe<Scalars['Boolean']['output']>;
  datumIngangOnderzoek?: Maybe<BrpDatum>;
};

export type Categorie = {
  __typename?: 'Categorie';
  naam: Scalars['String']['output'];
  url: Scalars['String']['output'];
  uuid: Scalars['String']['output'];
};

export type CategorieRelatie = {
  __typename?: 'CategorieRelatie';
  beginDatum?: Maybe<Scalars['Date']['output']>;
  categorie?: Maybe<Categorie>;
  eindDatum?: Maybe<Scalars['Date']['output']>;
  partij?: Maybe<OpenKlant2ForeignKey>;
  url: Scalars['String']['output'];
  uuid: Scalars['String']['output'];
};

export type CategorieRelatieForeignKey = {
  __typename?: 'CategorieRelatieForeignKey';
  beginDatum?: Maybe<Scalars['Date']['output']>;
  categorieNaam: Scalars['String']['output'];
  eindDatum?: Maybe<Scalars['Date']['output']>;
  url: Scalars['String']['output'];
  uuid: Scalars['String']['output'];
};

export type ContactenPage = {
  __typename?: 'ContactenPage';
  /**  The elements on this page */
  content: Array<OpenProductContact>;
  /**  The requested page number */
  number: Scalars['Int']['output'];
  /**  The number of elements on this page */
  numberOfElements: Scalars['Int']['output'];
  /**  The requested page size */
  size: Scalars['Int']['output'];
  /**  The total number of elements */
  totalElements: Scalars['Int']['output'];
  /**  The total number of available pages */
  totalPages: Scalars['Int']['output'];
};

export type Contactnaam = {
  __typename?: 'Contactnaam';
  achternaam?: Maybe<Scalars['String']['output']>;
  voorletters?: Maybe<Scalars['String']['output']>;
  voornaam?: Maybe<Scalars['String']['output']>;
  voorvoegselAchternaam?: Maybe<Scalars['String']['output']>;
};

export type ContactnaamInput = {
  achternaam?: InputMaybe<Scalars['String']['input']>;
  voorletters?: InputMaybe<Scalars['String']['input']>;
  voornaam?: InputMaybe<Scalars['String']['input']>;
  voorvoegselAchternaam?: InputMaybe<Scalars['String']['input']>;
};

export type ContactpersoonIdentificatie = {
  __typename?: 'ContactpersoonIdentificatie';
  contactnaam?: Maybe<Contactnaam>;
  uuid?: Maybe<Scalars['UUID']['output']>;
  volledigeNaam?: Maybe<Scalars['String']['output']>;
  werkteVoorPartij?: Maybe<OpenKlant2ForeignKey>;
};

export type ContactpersoonIdentificatieInput = {
  contactnaam?: InputMaybe<ContactnaamInput>;
  uuid?: InputMaybe<Scalars['UUID']['input']>;
  volledigeNaam?: InputMaybe<Scalars['String']['input']>;
  werkteVoorPartij?: InputMaybe<OpenKlant2ForeignKeyInput>;
};

export type DigitaleAdresRequestInput = {
  omschrijving: Scalars['String']['input'];
  type: DigitaleAdresType;
  uuid?: InputMaybe<Scalars['UUID']['input']>;
  verificatieCode?: InputMaybe<Scalars['String']['input']>;
  verificatieDatum?: InputMaybe<Scalars['DateTime']['input']>;
  waarde: Scalars['String']['input'];
};

export type DigitaleAdresResponse = {
  __typename?: 'DigitaleAdresResponse';
  isStandaardAdres?: Maybe<Scalars['Boolean']['output']>;
  omschrijving: Scalars['String']['output'];
  referentie: Scalars['String']['output'];
  type: DigitaleAdresType;
  uuid?: Maybe<Scalars['UUID']['output']>;
  verificatieCodeVerified?: Maybe<Scalars['Boolean']['output']>;
  verificatieDatum?: Maybe<Scalars['Date']['output']>;
  verificatieNeeded?: Maybe<Scalars['Boolean']['output']>;
  waarde: Scalars['String']['output'];
};

export enum DigitaleAdresType {
  Email = 'EMAIL',
  Overig = 'OVERIG',
  Telefoonnummer = 'TELEFOONNUMMER'
}

export type DirectPaymentRequestInput = {
  amount: Scalars['PositiveFloat']['input'];
  identifier: Scalars['String']['input'];
  langId?: InputMaybe<Scalars['String']['input']>;
  orderId: Scalars['String']['input'];
  reference: Scalars['String']['input'];
  returnUrl?: InputMaybe<Scalars['String']['input']>;
};

export type DirectPaymentResponse = {
  __typename?: 'DirectPaymentResponse';
  redirectUrl: Scalars['String']['output'];
};

export type DirectPaymentStatus = {
  __typename?: 'DirectPaymentStatus';
  status?: Maybe<DirectPaymentStatusCategory>;
};

export enum DirectPaymentStatusCategory {
  Rejected = 'REJECTED',
  StatusUnknown = 'STATUS_UNKNOWN',
  Successful = 'SUCCESSFUL'
}

export type Document = {
  __typename?: 'Document';
  bestandsnaam?: Maybe<Scalars['String']['output']>;
  bestandsomvang?: Maybe<Scalars['Int']['output']>;
  creatiedatum?: Maybe<Scalars['String']['output']>;
  documentapi: Scalars['String']['output'];
  formaat?: Maybe<Scalars['String']['output']>;
  identificatie?: Maybe<Scalars['String']['output']>;
  titel?: Maybe<Scalars['String']['output']>;
  uuid: Scalars['UUID']['output'];
};

export type Eigenaar = {
  __typename?: 'Eigenaar';
  rechtsvorm: Scalars['String']['output'];
  rsin?: Maybe<Scalars['String']['output']>;
  uitgebreideRechtsvorm: Scalars['String']['output'];
};

export type Embedded = {
  __typename?: 'Embedded';
  eigenaar: Eigenaar;
  vestiging?: Maybe<Vestiging>;
};

export type FormDefinition = {
  __typename?: 'FormDefinition';
  formDefinition: Scalars['JSON']['output'];
};

export type GemachtigdeV2 = {
  __typename?: 'GemachtigdeV2';
  bedrijf?: Maybe<MaatschappelijkeActiviteit>;
  persoon?: Maybe<BrpPersoon>;
};

export type HadBetrokkenActoren = {
  __typename?: 'HadBetrokkenActoren';
  actoridentificator: OpenKlant2Identificator;
  indicatieActief: Scalars['Boolean']['output'];
  naam: Scalars['String']['output'];
  soortActor: Scalars['String']['output'];
  url: Scalars['String']['output'];
  uuid: Scalars['String']['output'];
};

export type HadKlantcontact = {
  __typename?: 'HadKlantcontact';
  gingOverOnderwerpobjecten: Array<OpenKlant2ForeignKey>;
  hadBetrokkenActoren: Array<HadBetrokkenActoren>;
  hadBetrokkenen: Array<OpenKlant2ForeignKey>;
  indicatieContactGelukt: Scalars['Boolean']['output'];
  inhoud: Scalars['String']['output'];
  kanaal: Scalars['String']['output'];
  leiddeTotInterneTaken: Array<OpenKlant2ForeignKey>;
  nummer?: Maybe<Scalars['String']['output']>;
  omvatteBijlagen: Array<OpenKlant2ForeignKey>;
  onderwerp: Scalars['String']['output'];
  plaatsgevondenOp: Scalars['String']['output'];
  referentienummer?: Maybe<Scalars['String']['output']>;
  taal: Scalars['String']['output'];
  url: Scalars['String']['output'];
  uuid: Scalars['String']['output'];
  vertrouwelijk: Scalars['Boolean']['output'];
};

export type HandelsNaam = {
  __typename?: 'HandelsNaam';
  naam: Scalars['String']['output'];
  volgorde: Scalars['Int']['output'];
};

export type LinksPage = {
  __typename?: 'LinksPage';
  /**  The elements on this page */
  content: Array<OpenProductLink>;
  /**  The requested page number */
  number: Scalars['Int']['output'];
  /**  The number of elements on this page */
  numberOfElements: Scalars['Int']['output'];
  /**  The requested page size */
  size: Scalars['Int']['output'];
  /**  The total number of elements */
  totalElements: Scalars['Int']['output'];
  /**  The total number of available pages */
  totalPages: Scalars['Int']['output'];
};

export type LocatiesPage = {
  __typename?: 'LocatiesPage';
  /**  The elements on this page */
  content: Array<OpenProductLocatie>;
  /**  The requested page number */
  number: Scalars['Int']['output'];
  /**  The number of elements on this page */
  numberOfElements: Scalars['Int']['output'];
  /**  The requested page size */
  size: Scalars['Int']['output'];
  /**  The total number of elements */
  totalElements: Scalars['Int']['output'];
  /**  The total number of available pages */
  totalPages: Scalars['Int']['output'];
};

export type MaatschappelijkeActiviteit = {
  __typename?: 'MaatschappelijkeActiviteit';
  embedded?: Maybe<Embedded>;
  formeleRegistratiedatum?: Maybe<Scalars['String']['output']>;
  handelsnamen?: Maybe<Array<Maybe<HandelsNaam>>>;
  indNonMailing?: Maybe<Scalars['String']['output']>;
  kvkNummer: Scalars['String']['output'];
  materieleRegistratie?: Maybe<MaterieleRegistratie>;
  naam: Scalars['String']['output'];
  sbiActiviteiten?: Maybe<Array<Maybe<SbiActiviteit>>>;
  statutaireNaam?: Maybe<Scalars['String']['output']>;
  totaalWerkzamePersonen?: Maybe<Scalars['Int']['output']>;
};

export type MaterieleRegistratie = {
  __typename?: 'MaterieleRegistratie';
  datumAanvang: Scalars['String']['output'];
};

export type Mutation = {
  __typename?: 'Mutation';
  /**  Create DigitaleAdres for User */
  createUserDigitaleAdres?: Maybe<DigitaleAdresResponse>;
  /**  Create Partij for user */
  createUserPartij?: Maybe<PartijResponse>;
  createVerificatie?: Maybe<VerificatieCreateResponse>;
  /**  Delete DigitaleAdres of User by Id */
  deleteUserDigitaleAdres?: Maybe<Scalars['Boolean']['output']>;
  /**  Do Worldline Direct payment */
  doDirectPayment: DirectPaymentResponse;
  /**  Submit a task */
  submitTaakV2: TaakV2;
  /**  Update product */
  updateProduct?: Maybe<OpenProductProduct>;
  /**  Update product verbruiks object */
  updateProductVerbruiksObject: ProductVerbruiksObject;
  /**  Update DigitaleAdres of User */
  updateUserDigitaleAdres?: Maybe<DigitaleAdresResponse>;
  /**  Update user Partij */
  updateUserPartij?: Maybe<PartijResponse>;
  verifyVerificatie?: Maybe<VerificatieVerifyResponse>;
};


export type MutationCreateUserDigitaleAdresArgs = {
  digitaleAdresRequest: DigitaleAdresRequestInput;
};


export type MutationCreateUserPartijArgs = {
  partijRequest: PartijRequestInput;
};


export type MutationCreateVerificatieArgs = {
  verificatieCreateInput: VerificatieCreateInput;
};


export type MutationDeleteUserDigitaleAdresArgs = {
  digitaleAdresId: Scalars['UUID']['input'];
};


export type MutationDoDirectPaymentArgs = {
  paymentRequest: DirectPaymentRequestInput;
};



export type MutationSubmitTaakV2Args = {
  id: Scalars['UUID']['input'];
  submission: Scalars['JSON']['input'];
};


export type MutationUpdateProductArgs = {
  productUpdateRequest: UpdateProductRequestInput;
};


export type MutationUpdateProductVerbruiksObjectArgs = {
  id: Scalars['UUID']['input'];
  submission: Scalars['JSON']['input'];
};


export type MutationUpdateUserDigitaleAdresArgs = {
  digitaleAdresRequest: DigitaleAdresRequestInput;
};


export type MutationUpdateUserPartijArgs = {
  partijRequest: PartijRequestInput;
};


export type MutationVerifyVerificatieArgs = {
  verificatieVerifyInput: VerificatieVerifyInput;
};

export type OgoneBetaling = {
  __typename?: 'OgoneBetaling';
  bedrag: Scalars['PositiveFloat']['output'];
  betaalkenmerk: Scalars['String']['output'];
  pspid: Scalars['String']['output'];
};

export enum OnderwerpObjectIndentificatorType {
  Product = 'PRODUCT',
  Zaak = 'ZAAK'
}

export type OpenKlant2Adres = {
  __typename?: 'OpenKlant2Adres';
  adresregel1?: Maybe<Scalars['String']['output']>;
  adresregel2?: Maybe<Scalars['String']['output']>;
  adresregel3?: Maybe<Scalars['String']['output']>;
  land?: Maybe<OpenKlant2Landcode>;
  nummeraanduidingId?: Maybe<Scalars['String']['output']>;
};

export type OpenKlant2DigitaleAdres = {
  __typename?: 'OpenKlant2DigitaleAdres';
  adres: Scalars['String']['output'];
  omschrijving: Scalars['String']['output'];
  referentie?: Maybe<Scalars['String']['output']>;
  soortDigitaalAdres: Scalars['String']['output'];
  url?: Maybe<Scalars['String']['output']>;
  uuid?: Maybe<Scalars['UUID']['output']>;
  verstrektDoorBetrokkene?: Maybe<OpenKlant2Uuid>;
  verstrektDoorPartij?: Maybe<OpenKlant2Uuid>;
};

export type OpenKlant2ForeignKey = {
  __typename?: 'OpenKlant2ForeignKey';
  url: Scalars['String']['output'];
  uuid: Scalars['UUID']['output'];
};

export type OpenKlant2ForeignKeyInput = {
  url: Scalars['String']['input'];
  uuid: Scalars['UUID']['input'];
};

export type OpenKlant2Identificator = {
  __typename?: 'OpenKlant2Identificator';
  codeObjecttype: Scalars['String']['output'];
  codeRegister: Scalars['String']['output'];
  codeSoortObjectId: Scalars['String']['output'];
  objectId: Scalars['String']['output'];
};

export type OpenKlant2IdentificeerdePartij = {
  __typename?: 'OpenKlant2IdentificeerdePartij';
  uuid: Scalars['UUID']['output'];
};

export type OpenKlant2Klantcontact = {
  __typename?: 'OpenKlant2Klantcontact';
  gingOverOnderwerpobjecten: Array<OpenKlant2ForeignKey>;
  hadBetrokkenActoren: Array<HadBetrokkenActoren>;
  hadBetrokkenen: Array<OpenKlant2ForeignKey>;
  indicatieContactGelukt: Scalars['Boolean']['output'];
  inhoud: Scalars['String']['output'];
  kanaal: Scalars['String']['output'];
  leiddeTotInterneTaken: Array<OpenKlant2ForeignKey>;
  metadata?: Maybe<Scalars['JSON']['output']>;
  nummer?: Maybe<Scalars['String']['output']>;
  omvatteBijlagen: Array<OpenKlant2ForeignKey>;
  onderwerp: Scalars['String']['output'];
  plaatsgevondenOp: Scalars['String']['output'];
  reactie?: Maybe<Scalars['String']['output']>;
  referentienummer?: Maybe<Scalars['String']['output']>;
  taal: Scalars['String']['output'];
  url: Scalars['String']['output'];
  uuid: Scalars['String']['output'];
  vertrouwelijk: Scalars['Boolean']['output'];
};

export enum OpenKlant2Landcode {
  Abessinie = 'ABESSINIE',
  AbuDhabi = 'ABU_DHABI',
  Aden = 'ADEN',
  Afghanistan = 'AFGHANISTAN',
  Ajman = 'AJMAN',
  Albanie = 'ALBANIE',
  Algerije = 'ALGERIJE',
  AmerikaanseMaagdeneilanden = 'AMERIKAANSE_MAAGDENEILANDEN',
  Amerikaanssamoa = 'AMERIKAANSSAMOA',
  Andorra = 'ANDORRA',
  Angola = 'ANGOLA',
  Anguilla = 'ANGUILLA',
  Antigua = 'ANTIGUA',
  AntiguaEnBarbuda = 'ANTIGUA_EN_BARBUDA',
  Argentinie = 'ARGENTINIE',
  Armenie = 'ARMENIE',
  Aruba = 'ARUBA',
  Ascension = 'ASCENSION',
  Australie = 'AUSTRALIE',
  AustralischeSalomonseilanden = 'AUSTRALISCHE_SALOMONSEILANDEN',
  AustralischNieuwguinea = 'AUSTRALISCH_NIEUWGUINEA',
  Azerbeidzjan = 'AZERBEIDZJAN',
  Azoren = 'AZOREN',
  Bahamas = 'BAHAMAS',
  Bahrein = 'BAHREIN',
  Bangladesh = 'BANGLADESH',
  Barbados = 'BARBADOS',
  Basutoland = 'BASUTOLAND',
  Bechuanaland = 'BECHUANALAND',
  Belarus = 'BELARUS',
  Belau = 'BELAU',
  Belgie = 'BELGIE',
  Belgischcongo = 'BELGISCHCONGO',
  Belize = 'BELIZE',
  Benin = 'BENIN',
  Bermuda = 'BERMUDA',
  Bhutan = 'BHUTAN',
  Bolivia = 'BOLIVIA',
  Bonaire = 'BONAIRE',
  BondsrepubliekDuitsland = 'BONDSREPUBLIEK_DUITSLAND',
  Bosnieherzegovina = 'BOSNIEHERZEGOVINA',
  Botswana = 'BOTSWANA',
  Brazilie = 'BRAZILIE',
  Britsafrika = 'BRITSAFRIKA',
  Britsborneo = 'BRITSBORNEO',
  BritseAntillen = 'BRITSE_ANTILLEN',
  BritseMaagdeneilanden = 'BRITSE_MAAGDENEILANDEN',
  BritseSalomonseilanden = 'BRITSE_SALOMONSEILANDEN',
  Britsguyana = 'BRITSGUYANA',
  Britshonduras = 'BRITSHONDURAS',
  Britsindie = 'BRITSINDIE',
  Britskameroen = 'BRITSKAMEROEN',
  Britssomaliland = 'BRITSSOMALILAND',
  BritsAntarctischTerritorium = 'BRITS_ANTARCTISCH_TERRITORIUM',
  BritsIndischeOceaanterritorium = 'BRITS_INDISCHE_OCEAANTERRITORIUM',
  BritsNoordborneo = 'BRITS_NOORDBORNEO',
  BritsOostafrika = 'BRITS_OOSTAFRIKA',
  BritsWestborneo = 'BRITS_WESTBORNEO',
  BritsWestindie = 'BRITS_WESTINDIE',
  Brunei = 'BRUNEI',
  Bulgarije = 'BULGARIJE',
  BurkinaFaso = 'BURKINA_FASO',
  Burma = 'BURMA',
  Burundi = 'BURUNDI',
  Cabinda = 'CABINDA',
  Caicoseilanden = 'CAICOSEILANDEN',
  Cambodja = 'CAMBODJA',
  Canada = 'CANADA',
  CanarischeEilanden = 'CANARISCHE_EILANDEN',
  CantonEnEnderbury = 'CANTON_EN_ENDERBURY',
  Caymaneilanden = 'CAYMANEILANDEN',
  CentraalafrikaanseRepubliek = 'CENTRAALAFRIKAANSE_REPUBLIEK',
  Ceylon = 'CEYLON',
  Chili = 'CHILI',
  China = 'CHINA',
  Christmaseiland = 'CHRISTMASEILAND',
  Cocoseilanden = 'COCOSEILANDEN',
  Colombia = 'COLOMBIA',
  Comoren = 'COMOREN',
  Congo = 'CONGO',
  Congobrazzaville = 'CONGOBRAZZAVILLE',
  Congokinshasa = 'CONGOKINSHASA',
  Cookeilanden = 'COOKEILANDEN',
  CostaRica = 'COSTA_RICA',
  Cuba = 'CUBA',
  Curacao = 'CURACAO',
  Cyprus = 'CYPRUS',
  Dahomey = 'DAHOMEY',
  Dantzig = 'DANTZIG',
  DemocratischeRepubliekCongo = 'DEMOCRATISCHE_REPUBLIEK_CONGO',
  Denemarken = 'DENEMARKEN',
  Djibouti = 'DJIBOUTI',
  Dominica = 'DOMINICA',
  DominicaanseRepubliek = 'DOMINICAANSE_REPUBLIEK',
  Dubai = 'DUBAI',
  DuitseDemocratischeRepubliek = 'DUITSE_DEMOCRATISCHE_REPUBLIEK',
  Duitsland = 'DUITSLAND',
  DuitsOostafrika = 'DUITS_OOSTAFRIKA',
  DuitsZuidwestafrika = 'DUITS_ZUIDWESTAFRIKA',
  Ecuador = 'ECUADOR',
  Egypte = 'EGYPTE',
  ElSalvador = 'EL_SALVADOR',
  Equatoriaalguinea = 'EQUATORIAALGUINEA',
  Eritrea = 'ERITREA',
  Estland = 'ESTLAND',
  Eswatini = 'ESWATINI',
  Ethiopie = 'ETHIOPIE',
  Faeroer = 'FAEROER',
  Falklandeilanden = 'FALKLANDEILANDEN',
  FederaleRepubliekJoegoslavie = 'FEDERALE_REPUBLIEK_JOEGOSLAVIE',
  Fiji = 'FIJI',
  Filipijnen = 'FILIPIJNEN',
  Finland = 'FINLAND',
  Frankrijk = 'FRANKRIJK',
  Franscongo = 'FRANSCONGO',
  Fransguyana = 'FRANSGUYANA',
  Fransindie = 'FRANSINDIE',
  Franskameroen = 'FRANSKAMEROEN',
  Franspolynesie = 'FRANSPOLYNESIE',
  Franssomaliland = 'FRANSSOMALILAND',
  FransEquatoriaalafrika = 'FRANS_EQUATORIAALAFRIKA',
  FransIndochina = 'FRANS_INDOCHINA',
  FransTerritoriumVoorAfarsEnIssas = 'FRANS_TERRITORIUM_VOOR_AFARS_EN_ISSAS',
  FransWestafrika = 'FRANS_WESTAFRIKA',
  Fujairah = 'FUJAIRAH',
  Gabon = 'GABON',
  Gambia = 'GAMBIA',
  GazastrookEnWestelijkeJordaanoever = 'GAZASTROOK_EN_WESTELIJKE_JORDAANOEVER',
  Georgie = 'GEORGIE',
  Ghana = 'GHANA',
  Gibraltar = 'GIBRALTAR',
  Gilberteilanden = 'GILBERTEILANDEN',
  GilbertEnElliceeilanden = 'GILBERT_EN_ELLICEEILANDEN',
  Goa = 'GOA',
  Goudkust = 'GOUDKUST',
  Grenada = 'GRENADA',
  Griekenland = 'GRIEKENLAND',
  Groenland = 'GROENLAND',
  Guadeloupe = 'GUADELOUPE',
  Guam = 'GUAM',
  Guatemala = 'GUATEMALA',
  Guinee = 'GUINEE',
  Guineebissau = 'GUINEEBISSAU',
  Guyana = 'GUYANA',
  Haiti = 'HAITI',
  Hawaiieilanden = 'HAWAIIEILANDEN',
  Honduras = 'HONDURAS',
  Hongarije = 'HONGARIJE',
  Hongkong = 'HONGKONG',
  Ierland = 'IERLAND',
  Ifni = 'IFNI',
  Ijsland = 'IJSLAND',
  India = 'INDIA',
  Indochina = 'INDOCHINA',
  Indonesie = 'INDONESIE',
  InternationaalGebied = 'INTERNATIONAAL_GEBIED',
  Irak = 'IRAK',
  Iran = 'IRAN',
  Israel = 'ISRAEL',
  Italiaanssomaliland = 'ITALIAANSSOMALILAND',
  Italie = 'ITALIE',
  Ivoorkust = 'IVOORKUST',
  Jamaica = 'JAMAICA',
  Japan = 'JAPAN',
  Jemen = 'JEMEN',
  Joegoslavie = 'JOEGOSLAVIE',
  Johnston = 'JOHNSTON',
  Johore = 'JOHORE',
  Jordanie = 'JORDANIE',
  Kaapverdie = 'KAAPVERDIE',
  KaapverdischeEilanden = 'KAAPVERDISCHE_EILANDEN',
  Kameroen = 'KAMEROEN',
  Kanaaleilanden = 'KANAALEILANDEN',
  Kashmir = 'KASHMIR',
  Kazachstan = 'KAZACHSTAN',
  Kedah = 'KEDAH',
  KeizerWilhelmsland = 'KEIZER_WILHELMSLAND',
  Kelantan = 'KELANTAN',
  Kenya = 'KENYA',
  Kirgizie = 'KIRGIZIE',
  Kiribati = 'KIRIBATI',
  Koeweit = 'KOEWEIT',
  Korea = 'KOREA',
  Kosovo = 'KOSOVO',
  Kroatie = 'KROATIE',
  Labuan = 'LABUAN',
  Laos = 'LAOS',
  Leewardeilanden = 'LEEWARDEILANDEN',
  Lesotho = 'LESOTHO',
  Letland = 'LETLAND',
  Libanon = 'LIBANON',
  Liberia = 'LIBERIA',
  Libie = 'LIBIE',
  Liechtenstein = 'LIECHTENSTEIN',
  Litouwen = 'LITOUWEN',
  Luxemburg = 'LUXEMBURG',
  Macau = 'MACAU',
  Macedonie = 'MACEDONIE',
  Madagaskar = 'MADAGASKAR',
  Madeiraeilanden = 'MADEIRAEILANDEN',
  Malakka = 'MALAKKA',
  Malawi = 'MALAWI',
  Maldiven = 'MALDIVEN',
  Maleisie = 'MALEISIE',
  Mali = 'MALI',
  Malta = 'MALTA',
  Man = 'MAN',
  Marianen = 'MARIANEN',
  Marokko = 'MAROKKO',
  Marshalleilanden = 'MARSHALLEILANDEN',
  Martinique = 'MARTINIQUE',
  Mauritanie = 'MAURITANIE',
  Mauritius = 'MAURITIUS',
  Mayotte = 'MAYOTTE',
  Mexico = 'MEXICO',
  Micronesia = 'MICRONESIA',
  Midway = 'MIDWAY',
  Moldavie = 'MOLDAVIE',
  Monaco = 'MONACO',
  Mongolie = 'MONGOLIE',
  Montenegro = 'MONTENEGRO',
  Montserrat = 'MONTSERRAT',
  Mozambique = 'MOZAMBIQUE',
  MuscatEnOman = 'MUSCAT_EN_OMAN',
  Myanmar = 'MYANMAR',
  Namibie = 'NAMIBIE',
  Nauru = 'NAURU',
  Nederland = 'NEDERLAND',
  NederlandseAntillen = 'NEDERLANDSE_ANTILLEN',
  Nederlandsindie = 'NEDERLANDSINDIE',
  NederlandsNieuwguinea = 'NEDERLANDS_NIEUWGUINEA',
  NegriSembilan = 'NEGRI_SEMBILAN',
  Nepal = 'NEPAL',
  Newfoundland = 'NEWFOUNDLAND',
  Nicaragua = 'NICARAGUA',
  Nieuwcaledonie = 'NIEUWCALEDONIE',
  NieuweHebriden = 'NIEUWE_HEBRIDEN',
  Nieuwzeeland = 'NIEUWZEELAND',
  Niger = 'NIGER',
  Nigeria = 'NIGERIA',
  Niue = 'NIUE',
  None = 'NONE',
  Noordjemen = 'NOORDJEMEN',
  Noordkorea = 'NOORDKOREA',
  Noordrhodesie = 'NOORDRHODESIE',
  Noordvietnam = 'NOORDVIETNAM',
  Noorwegen = 'NOORWEGEN',
  Norfolk = 'NORFOLK',
  Nyasaland = 'NYASALAND',
  Oekraine = 'OEKRAINE',
  Oezbekistan = 'OEZBEKISTAN',
  Oman = 'OMAN',
  Oostenrijk = 'OOSTENRIJK',
  Oostenrijkhongarije = 'OOSTENRIJKHONGARIJE',
  Oppervolta = 'OPPERVOLTA',
  Pacificeilanden = 'PACIFICEILANDEN',
  Pahang = 'PAHANG',
  Pakistan = 'PAKISTAN',
  Palau = 'PALAU',
  Palestina = 'PALESTINA',
  Panama = 'PANAMA',
  Panamakanaalzone = 'PANAMAKANAALZONE',
  Papoeanieuwguinea = 'PAPOEANIEUWGUINEA',
  Paraguay = 'PARAGUAY',
  Perak = 'PERAK',
  Perlis = 'PERLIS',
  Peru = 'PERU',
  Phoenixeilanden = 'PHOENIXEILANDEN',
  Pitcairneilanden = 'PITCAIRNEILANDEN',
  Polen = 'POLEN',
  Portugal = 'PORTUGAL',
  Portugeesafrika = 'PORTUGEESAFRIKA',
  Portugeesguinee = 'PORTUGEESGUINEE',
  Portugeesindie = 'PORTUGEESINDIE',
  Portugeestimor = 'PORTUGEESTIMOR',
  PortugeesOostafrika = 'PORTUGEES_OOSTAFRIKA',
  PortugeesWestafrika = 'PORTUGEES_WESTAFRIKA',
  PuertoRico = 'PUERTO_RICO',
  Qatar = 'QATAR',
  RasAlkhaimah = 'RAS_ALKHAIMAH',
  RepubliekNoordmacedonie = 'REPUBLIEK_NOORDMACEDONIE',
  Reunion = 'REUNION',
  Rhodesie = 'RHODESIE',
  Riukiueilanden = 'RIUKIUEILANDEN',
  Roemenie = 'ROEMENIE',
  Ruandaurundi = 'RUANDAURUNDI',
  Rusland = 'RUSLAND',
  RuslandOud = 'RUSLAND_OUD',
  Rwanda = 'RWANDA',
  Saarland = 'SAARLAND',
  Saba = 'SABA',
  Sabah = 'SABAH',
  SaintKittsEnNevis = 'SAINT_KITTS_EN_NEVIS',
  SaintKittsNevisEnAnguilla = 'SAINT_KITTS_NEVIS_EN_ANGUILLA',
  SaintLucia = 'SAINT_LUCIA',
  SaintPierreEnMiquelon = 'SAINT_PIERRE_EN_MIQUELON',
  SaintVincent = 'SAINT_VINCENT',
  SaintVincentEnDeGrenadines = 'SAINT_VINCENT_EN_DE_GRENADINES',
  Salomonseilanden = 'SALOMONSEILANDEN',
  Samoa = 'SAMOA',
  SanMarino = 'SAN_MARINO',
  Saoediarabie = 'SAOEDIARABIE',
  SaoTomeEnPrincipe = 'SAO_TOME_EN_PRINCIPE',
  Sarawak = 'SARAWAK',
  Selangor = 'SELANGOR',
  Senegal = 'SENEGAL',
  Servie = 'SERVIE',
  ServieEnMontenegro = 'SERVIE_EN_MONTENEGRO',
  Seychellen = 'SEYCHELLEN',
  SeychellenEnAmiranten = 'SEYCHELLEN_EN_AMIRANTEN',
  Sharjah = 'SHARJAH',
  Siam = 'SIAM',
  SierraLeone = 'SIERRA_LEONE',
  Sikkim = 'SIKKIM',
  Singapore = 'SINGAPORE',
  Sinthelena = 'SINTHELENA',
  SintEustatius = 'SINT_EUSTATIUS',
  SintMaarten = 'SINT_MAARTEN',
  Slovenie = 'SLOVENIE',
  Slowakije = 'SLOWAKIJE',
  Soedan = 'SOEDAN',
  Somalie = 'SOMALIE',
  Sovjetunie = 'SOVJETUNIE',
  SpaanseSahara = 'SPAANSE_SAHARA',
  Spaansguinee = 'SPAANSGUINEE',
  SpaansNoordafrika = 'SPAANS_NOORDAFRIKA',
  Spanje = 'SPANJE',
  Spitsbergen = 'SPITSBERGEN',
  SriLanka = 'SRI_LANKA',
  StraitsSettlements = 'STRAITS_SETTLEMENTS',
  Suriname = 'SURINAME',
  Svalbardeilanden = 'SVALBARDEILANDEN',
  Swaziland = 'SWAZILAND',
  Syrie = 'SYRIE',
  Tadzjikistan = 'TADZJIKISTAN',
  Taiwan = 'TAIWAN',
  Tanganyika = 'TANGANYIKA',
  Tanzania = 'TANZANIA',
  Tasmanie = 'TASMANIE',
  Thailand = 'THAILAND',
  Tibet = 'TIBET',
  TimorLeste = 'TIMOR_LESTE',
  Togo = 'TOGO',
  Tokelau = 'TOKELAU',
  Tonga = 'TONGA',
  Transjordanie = 'TRANSJORDANIE',
  Trengganu = 'TRENGGANU',
  TrinidadEnTobago = 'TRINIDAD_EN_TOBAGO',
  TristanDaCunha = 'TRISTAN_DA_CUNHA',
  TrucialOman = 'TRUCIAL_OMAN',
  Tsjaad = 'TSJAAD',
  Tsjechie = 'TSJECHIE',
  Tsjechoslowakije = 'TSJECHOSLOWAKIJE',
  Tunesie = 'TUNESIE',
  Turkije = 'TURKIJE',
  Turkmenistan = 'TURKMENISTAN',
  Turkseilanden = 'TURKSEILANDEN',
  TurksEnCaicoseilanden = 'TURKS_EN_CAICOSEILANDEN',
  Tuvalu = 'TUVALU',
  Uganda = 'UGANDA',
  UmmAlqaiwain = 'UMM_ALQAIWAIN',
  Uruguay = 'URUGUAY',
  Urundi = 'URUNDI',
  Vanuatu = 'VANUATU',
  Vaticaanstad = 'VATICAANSTAD',
  Venezuela = 'VENEZUELA',
  VerenigdeArabischeEmiraten = 'VERENIGDE_ARABISCHE_EMIRATEN',
  VerenigdeArabischeRepubliek = 'VERENIGDE_ARABISCHE_REPUBLIEK',
  VerenigdeStatenVanAmerika = 'VERENIGDE_STATEN_VAN_AMERIKA',
  VerenigdKoninkrijk = 'VERENIGD_KONINKRIJK',
  Vietnam = 'VIETNAM',
  Wake = 'WAKE',
  WallisEnFutuna = 'WALLIS_EN_FUTUNA',
  WestelijkeSahara = 'WESTELIJKE_SAHARA',
  Westsamoa = 'WESTSAMOA',
  Windwardeilanden = 'WINDWARDEILANDEN',
  Zaire = 'ZAIRE',
  Zambia = 'ZAMBIA',
  Zanzibar = 'ZANZIBAR',
  Zimbabwe = 'ZIMBABWE',
  Zuidafrika = 'ZUIDAFRIKA',
  ZuidarabischeFederatie = 'ZUIDARABISCHE_FEDERATIE',
  Zuidjemen = 'ZUIDJEMEN',
  Zuidkorea = 'ZUIDKOREA',
  Zuidrhodesie = 'ZUIDRHODESIE',
  Zuidsoedan = 'ZUIDSOEDAN',
  Zuidvietnam = 'ZUIDVIETNAM',
  Zuidwestafrika = 'ZUIDWESTAFRIKA',
  Zweden = 'ZWEDEN',
  Zwitserland = 'ZWITSERLAND'
}

/**  A Type that represents a Klantinteracties API Partij object */
export type OpenKlant2Partij = {
  __typename?: 'OpenKlant2Partij';
  betrokkenen?: Maybe<Array<OpenKlant2ForeignKey>>;
  bezoekadres?: Maybe<OpenKlant2Adres>;
  categorieRelaties?: Maybe<Array<CategorieRelatieForeignKey>>;
  correspondentieadres?: Maybe<OpenKlant2Adres>;
  digitaleAdressen?: Maybe<Array<OpenKlant2ForeignKey>>;
  expand?: Maybe<PartijExpand>;
  indicatieActief: Scalars['Boolean']['output'];
  indicatieGeheimhouding?: Maybe<Scalars['Boolean']['output']>;
  interneNotitie?: Maybe<Scalars['String']['output']>;
  nummer?: Maybe<Scalars['String']['output']>;
  partijIdentificatie: PartijIdentificatie;
  partijIdentificatoren?: Maybe<Array<OpenKlant2PartijIdentificator>>;
  rekeningnummers?: Maybe<Array<OpenKlant2ForeignKey>>;
  soortPartij: SoortPartij;
  url?: Maybe<Scalars['String']['output']>;
  uuid?: Maybe<Scalars['UUID']['output']>;
  vertegenwoordigden?: Maybe<Array<OpenKlant2ForeignKey>>;
  voorkeursDigitaalAdres?: Maybe<OpenKlant2ForeignKey>;
  voorkeursRekeningnummer?: Maybe<OpenKlant2ForeignKey>;
  voorkeurstaal?: Maybe<Scalars['String']['output']>;
};

export type OpenKlant2PartijIdentificator = {
  __typename?: 'OpenKlant2PartijIdentificator';
  anderePartijIdentificator?: Maybe<Scalars['String']['output']>;
  identificeerdePartij?: Maybe<OpenKlant2IdentificeerdePartij>;
  partijIdentificator?: Maybe<OpenKlant2Identificator>;
  subIdentificatorVan?: Maybe<OpenKlant2SubIdentificatorVan>;
  url?: Maybe<Scalars['String']['output']>;
  uuid?: Maybe<Scalars['UUID']['output']>;
};

export type OpenKlant2SubIdentificatorVan = {
  __typename?: 'OpenKlant2SubIdentificatorVan';
  uuid: Scalars['UUID']['output'];
};

export type OpenKlant2Uuid = {
  __typename?: 'OpenKlant2UUID';
  uuid: Scalars['UUID']['output'];
};

export type OpenProductActie = {
  __typename?: 'OpenProductActie';
  naam: Scalars['String']['output'];
  productTypeUuid?: Maybe<Scalars['UUID']['output']>;
  uuid: Scalars['UUID']['output'];
};

export type OpenProductBestand = {
  __typename?: 'OpenProductBestand';
  bestand: Scalars['String']['output'];
  productTypeUuid: Scalars['UUID']['output'];
  uuid: Scalars['UUID']['output'];
};

export type OpenProductContact = {
  __typename?: 'OpenProductContact';
  email?: Maybe<Scalars['String']['output']>;
  huisnummer?: Maybe<Scalars['String']['output']>;
  naam: Scalars['String']['output'];
  organisatie?: Maybe<OpenProductOrganisatie>;
  postcode?: Maybe<Scalars['String']['output']>;
  stad?: Maybe<Scalars['String']['output']>;
  straat?: Maybe<Scalars['String']['output']>;
  telefoonnummer?: Maybe<Scalars['String']['output']>;
  uuid: Scalars['UUID']['output'];
};

export type OpenProductContentElement = {
  __typename?: 'OpenProductContentElement';
  content: Scalars['String']['output'];
  labels?: Maybe<Array<Scalars['String']['output']>>;
  taal: Scalars['String']['output'];
  uuid: Scalars['UUID']['output'];
};

export enum OpenProductDoelgroep {
  BedrijvenEnInstellingen = 'BEDRIJVEN_EN_INSTELLINGEN',
  Burgers = 'BURGERS',
  InterneOrganisatie = 'INTERNE_ORGANISATIE',
  Samenwerkingspartner = 'SAMENWERKINGSPARTNER'
}

export type OpenProductEmbeddedThema = {
  __typename?: 'OpenProductEmbeddedThema';
  aanmaakDatum: Scalars['ZonedDateTime']['output'];
  beschrijving?: Maybe<Scalars['String']['output']>;
  gepubliceerd?: Maybe<Scalars['Boolean']['output']>;
  hoofdThema?: Maybe<Scalars['String']['output']>;
  naam: Scalars['String']['output'];
  updateDatum: Scalars['ZonedDateTime']['output'];
  uuid: Scalars['UUID']['output'];
};

export enum OpenProductFrequentie {
  Eenmalig = 'EENMALIG',
  Geen = 'GEEN',
  Jaarlijks = 'JAARLIJKS',
  Maandelijks = 'MAANDELIJKS'
}

export type OpenProductLink = {
  __typename?: 'OpenProductLink';
  naam: Scalars['String']['output'];
  url: Scalars['String']['output'];
  uuid: Scalars['UUID']['output'];
};

export type OpenProductLocatie = {
  __typename?: 'OpenProductLocatie';
  email?: Maybe<Scalars['String']['output']>;
  huisnummer?: Maybe<Scalars['String']['output']>;
  naam: Scalars['String']['output'];
  postcode?: Maybe<Scalars['String']['output']>;
  stad?: Maybe<Scalars['String']['output']>;
  straat?: Maybe<Scalars['String']['output']>;
  telefoonnummer?: Maybe<Scalars['String']['output']>;
  uuid: Scalars['UUID']['output'];
};

export type OpenProductOrganisatie = {
  __typename?: 'OpenProductOrganisatie';
  code: Scalars['String']['output'];
  email?: Maybe<Scalars['String']['output']>;
  huisnummer?: Maybe<Scalars['String']['output']>;
  naam: Scalars['String']['output'];
  postcode?: Maybe<Scalars['String']['output']>;
  stad?: Maybe<Scalars['String']['output']>;
  straat?: Maybe<Scalars['String']['output']>;
  telefoonnummer?: Maybe<Scalars['String']['output']>;
  uuid: Scalars['UUID']['output'];
};

export type OpenProductPrefillResponse = {
  __typename?: 'OpenProductPrefillResponse';
  formulierUrl?: Maybe<Scalars['String']['output']>;
  hash?: Maybe<Scalars['String']['output']>;
  objectId?: Maybe<Scalars['UUID']['output']>;
};

export type OpenProductPrijs = {
  __typename?: 'OpenProductPrijs';
  actiefVanaf: Scalars['Date']['output'];
  prijsopties: Array<OpenProductPrijsOptie>;
  prijsregels: Array<OpenProductPrijsRegel>;
  uuid: Scalars['UUID']['output'];
};

export type OpenProductPrijsOptie = {
  __typename?: 'OpenProductPrijsOptie';
  bedrag: Scalars['PositiveFloat']['output'];
  beschrijving: Scalars['String']['output'];
  uuid: Scalars['UUID']['output'];
};

export type OpenProductPrijsRegel = {
  __typename?: 'OpenProductPrijsRegel';
  beschrijving: Scalars['String']['output'];
  url: Scalars['String']['output'];
  uuid: Scalars['UUID']['output'];
};

export type OpenProductProduct = {
  __typename?: 'OpenProductProduct';
  aanmaakDatum: Scalars['ZonedDateTime']['output'];
  aanvraagZaakUrl?: Maybe<Scalars['String']['output']>;
  aanvraagZaakUrn?: Maybe<Scalars['String']['output']>;
  acties?: Maybe<Array<OpenProductActie>>;
  content?: Maybe<Array<OpenProductContentElement>>;
  dataobject?: Maybe<Scalars['JSON']['output']>;
  decisions: Array<Scalars['JSON']['output']>;
  documenten?: Maybe<Array<Document>>;
  eindDatum?: Maybe<Scalars['Date']['output']>;
  frequentie: OpenProductFrequentie;
  gepubliceerd?: Maybe<Scalars['Boolean']['output']>;
  naam: Scalars['String']['output'];
  prijs?: Maybe<Scalars['Float']['output']>;
  producttype: OpenProductProductProductType;
  startDatum?: Maybe<Scalars['Date']['output']>;
  status: OpenProductToegestaneStatus;
  taken?: Maybe<Array<TaakV2>>;
  updateDatum: Scalars['ZonedDateTime']['output'];
  url?: Maybe<Scalars['String']['output']>;
  uuid: Scalars['UUID']['output'];
  verbruiksobject?: Maybe<Scalars['JSON']['output']>;
  zaken?: Maybe<Array<Zaak>>;
};

export type OpenProductProductProductType = {
  __typename?: 'OpenProductProductProductType';
  aanmaakDatum: Scalars['ZonedDateTime']['output'];
  code: Scalars['String']['output'];
  gepubliceerd?: Maybe<Scalars['Boolean']['output']>;
  keywords: Array<Scalars['String']['output']>;
  publicatieEindDatum?: Maybe<Scalars['Date']['output']>;
  publicatieStartDatum?: Maybe<Scalars['Date']['output']>;
  themas?: Maybe<Array<OpenProductEmbeddedThema>>;
  toegestaneStatussen: Array<OpenProductToegestaneStatus>;
  uniformeProductNaam: Scalars['String']['output'];
  updateDatum: Scalars['ZonedDateTime']['output'];
  uuid: Scalars['UUID']['output'];
};

export type OpenProductProductType = {
  __typename?: 'OpenProductProductType';
  aanmaakDatum: Scalars['ZonedDateTime']['output'];
  acties: Array<OpenProductActie>;
  bestanden: Array<OpenProductProductTypeBestand>;
  code: Scalars['String']['output'];
  contacten: Array<OpenProductContact>;
  content?: Maybe<Array<OpenProductContentElement>>;
  dataObjectSchema?: Maybe<OpenProductSchema>;
  doelgroep?: Maybe<OpenProductDoelgroep>;
  externCodes: Array<OpenProductProductTypeExterneCode>;
  gepubliceerd?: Maybe<Scalars['Boolean']['output']>;
  interneOpmerking?: Maybe<Scalars['String']['output']>;
  keywords: Array<Scalars['String']['output']>;
  links: Array<OpenProductLink>;
  locaties: Array<OpenProductLocatie>;
  naam: Scalars['String']['output'];
  organisaties: Array<OpenProductOrganisatie>;
  parameters: Array<OpenProductProductTypeParameter>;
  prijzen: Array<OpenProductPrijs>;
  processen: Array<OpenProductUrl>;
  publicatieEindDatum?: Maybe<Scalars['Date']['output']>;
  publicatieStartDatum?: Maybe<Scalars['Date']['output']>;
  samenvatting: Scalars['String']['output'];
  taal: Scalars['String']['output'];
  themas: Array<OpenProductProductTypeThema>;
  toegestaneStatussen: Array<OpenProductToegestaneStatus>;
  uniformeProductNaam: Scalars['String']['output'];
  updateDatum: Scalars['ZonedDateTime']['output'];
  uuid: Scalars['UUID']['output'];
  verbruiksObjectSchema?: Maybe<OpenProductSchema>;
  verzoektypen: Array<OpenProductUrl>;
  zaaktypen: Array<OpenProductUrl>;
};

export type OpenProductProductTypeBestand = {
  __typename?: 'OpenProductProductTypeBestand';
  bestand: Scalars['String']['output'];
  uuid: Scalars['UUID']['output'];
};

export type OpenProductProductTypeExterneCode = {
  __typename?: 'OpenProductProductTypeExterneCode';
  code: Scalars['String']['output'];
  naam: Scalars['String']['output'];
};

export type OpenProductProductTypeParameter = {
  __typename?: 'OpenProductProductTypeParameter';
  naam: Scalars['String']['output'];
  waarde: Scalars['String']['output'];
};

export type OpenProductProductTypeThema = {
  __typename?: 'OpenProductProductTypeThema';
  aanmaakDatum: Scalars['ZonedDateTime']['output'];
  beschrijving?: Maybe<Scalars['String']['output']>;
  gepubliceerd?: Maybe<Scalars['Boolean']['output']>;
  hoofdThema?: Maybe<Scalars['String']['output']>;
  naam: Scalars['String']['output'];
  updateDatum: Scalars['ZonedDateTime']['output'];
  uuid: Scalars['UUID']['output'];
};

export type OpenProductSchema = {
  __typename?: 'OpenProductSchema';
  naam: Scalars['String']['output'];
  schema: Scalars['JSON']['output'];
};

export type OpenProductThema = {
  __typename?: 'OpenProductThema';
  aanmaakDatum: Scalars['ZonedDateTime']['output'];
  beschrijving?: Maybe<Scalars['String']['output']>;
  gepubliceerd?: Maybe<Scalars['Boolean']['output']>;
  /**  UUID of the hoofdthema, which this thema is related to. */
  hoofdThema?: Maybe<Scalars['UUID']['output']>;
  links?: Maybe<Array<OpenProductLink>>;
  naam: Scalars['String']['output'];
  producten?: Maybe<Array<OpenProductProduct>>;
  producttypen: Array<OpenProductThemaProductType>;
  taken?: Maybe<Array<TaakV2>>;
  updateDatum: Scalars['ZonedDateTime']['output'];
  uuid: Scalars['UUID']['output'];
  zaken?: Maybe<Array<Zaak>>;
};

export type OpenProductThemaHierarchy = {
  __typename?: 'OpenProductThemaHierarchy';
  subThemas?: Maybe<Array<OpenProductThemaHierarchy>>;
  thema: OpenProductThema;
};

export type OpenProductThemaProductType = {
  __typename?: 'OpenProductThemaProductType';
  aanmaakDatum: Scalars['ZonedDateTime']['output'];
  code: Scalars['String']['output'];
  gepubliceerd?: Maybe<Scalars['Boolean']['output']>;
  keywords: Array<Scalars['String']['output']>;
  toegestaneStatussen: Array<OpenProductToegestaneStatus>;
  uniformeProductNaam: Scalars['String']['output'];
  updateDatum: Scalars['ZonedDateTime']['output'];
  uuid: Scalars['UUID']['output'];
};

export enum OpenProductToegestaneStatus {
  Actief = 'ACTIEF',
  Gereed = 'GEREED',
  Geweigerd = 'GEWEIGERD',
  Ingetrokken = 'INGETROKKEN',
  Initieel = 'INITIEEL',
  InAanvraag = 'IN_AANVRAAG',
  Verlopen = 'VERLOPEN'
}

export type OpenProductUrl = {
  __typename?: 'OpenProductUrl';
  url?: Maybe<Scalars['String']['output']>;
  urn?: Maybe<Scalars['String']['output']>;
};

export type OrganisatieIdentificatie = {
  __typename?: 'OrganisatieIdentificatie';
  naam?: Maybe<Scalars['String']['output']>;
};

export type OrganisatieIdentificatieInput = {
  naam?: InputMaybe<Scalars['String']['input']>;
};

export type OrganisatiesPage = {
  __typename?: 'OrganisatiesPage';
  /**  The elements on this page */
  content: Array<OpenProductOrganisatie>;
  /**  The requested page number */
  number: Scalars['Int']['output'];
  /**  The number of elements on this page */
  numberOfElements: Scalars['Int']['output'];
  /**  The requested page size */
  size: Scalars['Int']['output'];
  /**  The total number of elements */
  totalElements: Scalars['Int']['output'];
  /**  The total number of available pages */
  totalPages: Scalars['Int']['output'];
};

export type PartijExpand = {
  __typename?: 'PartijExpand';
  betrokkenen?: Maybe<Array<Betrokkene>>;
  categorieRelaties?: Maybe<Array<CategorieRelatie>>;
  digitaleAdressen?: Maybe<Array<OpenKlant2DigitaleAdres>>;
  hadKlantcontact?: Maybe<Array<HadKlantcontact>>;
};

export type PartijIdentificatie = ContactpersoonIdentificatie | OrganisatieIdentificatie | PersoonsIdentificatie;

export type PartijRequestInput = {
  contactpersoonIdentificatie?: InputMaybe<ContactpersoonIdentificatieInput>;
  indicatieActief: Scalars['Boolean']['input'];
  indicatieGeheimhouding: Scalars['Boolean']['input'];
  organisatieIdentificatie?: InputMaybe<OrganisatieIdentificatieInput>;
  persoonsIdentificatie?: InputMaybe<PersoonsIdentificatieInput>;
  type: PartijType;
};

export type PartijResponse = {
  __typename?: 'PartijResponse';
  contactpersoonIdentificatie?: Maybe<ContactpersoonIdentificatie>;
  digitaleAdressen?: Maybe<Array<OpenKlant2DigitaleAdres>>;
  indicatieActief: Scalars['Boolean']['output'];
  indicatieGeheimhouding?: Maybe<Scalars['Boolean']['output']>;
  klantcontacten?: Maybe<Array<HadKlantcontact>>;
  organisatieIdentificatie?: Maybe<OrganisatieIdentificatie>;
  persoonsIdentificatie?: Maybe<PersoonsIdentificatie>;
  type: PartijType;
};

export enum PartijType {
  Contactpersoon = 'CONTACTPERSOON',
  Organisatie = 'ORGANISATIE',
  Persoon = 'PERSOON'
}

export type PersoonsIdentificatie = {
  __typename?: 'PersoonsIdentificatie';
  contactnaam?: Maybe<Contactnaam>;
  volledigeNaam?: Maybe<Scalars['String']['output']>;
};

export type PersoonsIdentificatieInput = {
  contactnaam?: InputMaybe<ContactnaamInput>;
  volledigeNaam?: InputMaybe<Scalars['String']['input']>;
};

export type PrefillResponse = {
  __typename?: 'PrefillResponse';
  formulierUrl: Scalars['String']['output'];
  hash: Scalars['String']['output'];
  objectId: Scalars['UUID']['output'];
};

export type PrijzenPage = {
  __typename?: 'PrijzenPage';
  /**  The elements on this page */
  content: Array<OpenProductPrijs>;
  /**  The requested page number */
  number: Scalars['Int']['output'];
  /**  The number of elements on this page */
  numberOfElements: Scalars['Int']['output'];
  /**  The requested page size */
  size: Scalars['Int']['output'];
  /**  The total number of elements */
  totalElements: Scalars['Int']['output'];
  /**  The total number of available pages */
  totalPages: Scalars['Int']['output'];
};

export type Product = {
  __typename?: 'Product';
  documenten: Array<Scalars['String']['output']>;
  eigenschappen?: Maybe<Scalars['JSON']['output']>;
  geldigTot?: Maybe<Scalars['LocalDateTime']['output']>;
  geldigVan: Scalars['LocalDateTime']['output'];
  id?: Maybe<Scalars['UUID']['output']>;
  naam: Scalars['String']['output'];
  parameters?: Maybe<Scalars['JSON']['output']>;
  productDetails?: Maybe<ProductDetails>;
  productSubType?: Maybe<Scalars['String']['output']>;
  productType?: Maybe<ProductType>;
  status: Scalars['String']['output'];
  taken: Array<TaakV2>;
  verbruiksobjecten: Array<ProductVerbruiksObject>;
  zaken: Array<Zaak>;
};

export type ProductDetails = {
  __typename?: 'ProductDetails';
  data: Array<Scalars['JSON']['output']>;
  id?: Maybe<Scalars['UUID']['output']>;
  productInstantie: Scalars['UUID']['output'];
};

export type ProductPage = {
  __typename?: 'ProductPage';
  /**  The elements on this page */
  content: Array<Product>;
  /**  The requested page number */
  number: Scalars['Int']['output'];
  /**  The number of elements on this page */
  numberOfElements: Scalars['Int']['output'];
  /**  The requested page size */
  size: Scalars['Int']['output'];
  /**  The total number of elements */
  totalElements: Scalars['Int']['output'];
  /**  The total number of available pages */
  totalPages: Scalars['Int']['output'];
};

export type ProductType = {
  __typename?: 'ProductType';
  /**  Get list of available beslistabellen, with their object configurations */
  beslistabelMappings?: Maybe<Array<Scalars['String']['output']>>;
  eigenschappen?: Maybe<Scalars['JSON']['output']>;
  id?: Maybe<Scalars['UUID']['output']>;
  naam: Scalars['String']['output'];
  omschrijving?: Maybe<Scalars['String']['output']>;
  parameters?: Maybe<Scalars['JSON']['output']>;
  /**  Get list of available forms to prefill, with their object configurations */
  prefillMappings?: Maybe<Scalars['JSON']['output']>;
  productSubType?: Maybe<Scalars['String']['output']>;
  zaaktypen: Array<Scalars['UUID']['output']>;
};

export type ProductTypesPage = {
  __typename?: 'ProductTypesPage';
  /**  The elements on this page */
  content: Array<OpenProductProductType>;
  /**  The requested page number */
  number: Scalars['Int']['output'];
  /**  The number of elements on this page */
  numberOfElements: Scalars['Int']['output'];
  /**  The requested page size */
  size: Scalars['Int']['output'];
  /**  The total number of elements */
  totalElements: Scalars['Int']['output'];
  /**  The total number of available pages */
  totalPages: Scalars['Int']['output'];
};

export type ProductVerbruiksObject = {
  __typename?: 'ProductVerbruiksObject';
  data?: Maybe<Scalars['JSON']['output']>;
  id?: Maybe<Scalars['UUID']['output']>;
  productInstantie: Scalars['String']['output'];
  soort?: Maybe<Scalars['String']['output']>;
};

export type ProductenPage = {
  __typename?: 'ProductenPage';
  /**  The elements on this page */
  content: Array<OpenProductProduct>;
  /**  The requested page number */
  number: Scalars['Int']['output'];
  /**  The number of elements on this page */
  numberOfElements: Scalars['Int']['output'];
  /**  The requested page size */
  size: Scalars['Int']['output'];
  /**  The total number of elements */
  totalElements: Scalars['Int']['output'];
  /**  The total number of available pages */
  totalPages: Scalars['Int']['output'];
};

export type Query = {
  __typename?: 'Query';
  /**  Find the Partij of the authenticated user. */
  findUserPartij?: Maybe<OpenKlant2Partij>;
  /**  Gets the bedrijf data */
  getBedrijf?: Maybe<MaatschappelijkeActiviteit>;
  /**  Gets a single Bericht by Id */
  getBericht?: Maybe<Bericht>;
  getBerichten: BerichtenPage;
  /**  Gets the number of people living in the same house of the adresseerbaarObjectIdentificatie */
  getBewonersAantalV2?: Maybe<Scalars['Int']['output']>;
  getDecision: Array<Scalars['JSON']['output']>;
  getDirectPaymentStatus: DirectPaymentStatus;
  /**  find single form definition from repository */
  getFormDefinitionByName?: Maybe<FormDefinition>;
  /**  Get the form definition for a task the authenticated user owns */
  getFormDefinitionByTaskId?: Maybe<FormDefinition>;
  /**  Gets the data of the gemachtigde */
  getGemachtigdeV2?: Maybe<GemachtigdeV2>;
  /**  Get a Open product type by id */
  getOpenProduct?: Maybe<OpenProductProduct>;
  /**  Get a actie */
  getOpenProductActie?: Maybe<OpenProductActie>;
  /**  Get decision by actie naam */
  getOpenProductActieDecision: Array<Scalars['JSON']['output']>;
  getOpenProductActiePrefill?: Maybe<OpenProductPrefillResponse>;
  /**  Get all acties */
  getOpenProductActies: ActiesPage;
  /**  Get a bestand */
  getOpenProductBestand?: Maybe<OpenProductBestand>;
  /**  Get all bestanden */
  getOpenProductBestanden: BestandenPage;
  /**  Get a contact */
  getOpenProductContact?: Maybe<OpenProductContact>;
  /**  Get all contacten */
  getOpenProductContacten: ContactenPage;
  /**  Get all hoofd themas */
  getOpenProductHoofdThemas: Array<OpenProductThema>;
  /**  Get all hoofd themas by producten */
  getOpenProductHoofdThemasByProducten: Array<OpenProductThema>;
  /**  Get a link */
  getOpenProductLink?: Maybe<OpenProductLink>;
  /**  Get all links */
  getOpenProductLinks: LinksPage;
  /**  Get a locatie */
  getOpenProductLocatie?: Maybe<OpenProductLocatie>;
  /**  Get all locaties */
  getOpenProductLocaties: LocatiesPage;
  /**  Get a organisatie */
  getOpenProductOrganisatie?: Maybe<OpenProductOrganisatie>;
  /**  Get all organisaties */
  getOpenProductOrganisaties: OrganisatiesPage;
  /**  Get a prijs */
  getOpenProductPrijs?: Maybe<OpenProductPrijs>;
  /**  Get all prijzen */
  getOpenProductPrijzen: PrijzenPage;
  /**  Get a thema */
  getOpenProductThema?: Maybe<OpenProductThema>;
  /**  Get thema hierarchy */
  getOpenProductThemaHierarchy: Array<OpenProductThemaHierarchy>;
  /**  Get taken of a thema, including their parent themas */
  getOpenProductThemaTaken: Array<TaakV2>;
  /**  Get zaken of a thema, including their parent themas */
  getOpenProductThemaZaken: Array<Zaak>;
  /**  Get all themas */
  getOpenProductThemas: ThemasPage;
  /**  Get all themas hierarchy */
  getOpenProductThemasHierarchy: Array<OpenProductThemaHierarchy>;
  /**  Get a Open product type by id */
  getOpenProductType?: Maybe<OpenProductProductType>;
  /**  Get all Open product types */
  getOpenProductTypes: ProductTypesPage;
  getOpenProducten: ProductenPage;
  /**  Get a Open producten type by thema id */
  getOpenProductenByThema: Array<OpenProductProduct>;
  /**  Gets the persoon data */
  getPersoonV2?: Maybe<BrpPersoon>;
  /**  Get product by id */
  getProduct?: Maybe<Product>;
  getProductDecision: Array<Scalars['JSON']['output']>;
  /**  Get list of taken by product name */
  getProductTaken: Array<TaakV2>;
  /**  Get productType by name */
  getProductType?: Maybe<ProductType>;
  /**  Get productTypes where the user has products */
  getProductTypes: Array<ProductType>;
  /**  Get list of verbruiksobjecten of product */
  getProductVerbruiksObjecten: Array<ProductVerbruiksObject>;
  getProductZaken: Array<Zaak>;
  getProducten: ProductPage;
  /**  Get task by id V2 */
  getTaakByIdV2?: Maybe<TaakV2>;
  /**  Get a list of tasks. Optional filter for zaak V2 */
  getTakenV2: TaakPageV2;
  /**  Returns the total amount of unopened Berichten */
  getUnopenedBerichtenCount: Scalars['Int']['output'];
  /**  Get DigitaleAdressen of authenticated user. */
  getUserDigitaleAdressen?: Maybe<Array<DigitaleAdresResponse>>;
  /**  Get KlantContact by id of authenticated user. */
  getUserKlantContact?: Maybe<OpenKlant2Klantcontact>;
  getUserKlantContacten: Array<OpenKlant2Klantcontact>;
  /**  Get Partij by Id for authenticated user. */
  getUserPartij?: Maybe<OpenKlant2Partij>;
  /**  Gets a zaak by id */
  getZaak: Zaak;
  getZaken: ZaakPage;
  productPrefill: PrefillResponse;
  verificatieConfig?: Maybe<VerificationConfig>;
};


export type QueryGetBerichtArgs = {
  id: Scalars['UUID']['input'];
};


export type QueryGetBerichtenArgs = {
  onderwerp?: InputMaybe<Scalars['String']['input']>;
  pageNumber?: InputMaybe<Scalars['Int']['input']>;
  pageSize?: InputMaybe<Scalars['Int']['input']>;
};


export type QueryGetDecisionArgs = {
  dmnVariables?: InputMaybe<Scalars['JSON']['input']>;
  key: Scalars['String']['input'];
  productName: Scalars['String']['input'];
  productTypeId?: InputMaybe<Scalars['UUID']['input']>;
  sources?: InputMaybe<Scalars['JSON']['input']>;
};


export type QueryGetDirectPaymentStatusArgs = {
  hostedCheckoutId: Scalars['String']['input'];
  identifier: Scalars['String']['input'];
};


export type QueryGetFormDefinitionByNameArgs = {
  name: Scalars['String']['input'];
};


export type QueryGetFormDefinitionByTaskIdArgs = {
  taskId: Scalars['UUID']['input'];
};


export type QueryGetOpenProductArgs = {
  id: Scalars['UUID']['input'];
};


export type QueryGetOpenProductActieArgs = {
  id: Scalars['UUID']['input'];
};


export type QueryGetOpenProductActieDecisionArgs = {
  naam: Scalars['String']['input'];
  productId: Scalars['UUID']['input'];
};


export type QueryGetOpenProductActiePrefillArgs = {
  naam: Scalars['String']['input'];
  productId: Scalars['UUID']['input'];
};


export type QueryGetOpenProductActiesArgs = {
  pageNumber?: InputMaybe<Scalars['Int']['input']>;
  pageSize?: InputMaybe<Scalars['Int']['input']>;
};


export type QueryGetOpenProductBestandArgs = {
  id: Scalars['UUID']['input'];
};


export type QueryGetOpenProductBestandenArgs = {
  naam?: InputMaybe<Scalars['String']['input']>;
  pageNumber?: InputMaybe<Scalars['Int']['input']>;
  pageSize?: InputMaybe<Scalars['Int']['input']>;
};


export type QueryGetOpenProductContactArgs = {
  id: Scalars['UUID']['input'];
};


export type QueryGetOpenProductContactenArgs = {
  naam?: InputMaybe<Scalars['String']['input']>;
  pageNumber?: InputMaybe<Scalars['Int']['input']>;
  pageSize?: InputMaybe<Scalars['Int']['input']>;
};


export type QueryGetOpenProductLinkArgs = {
  id: Scalars['UUID']['input'];
};


export type QueryGetOpenProductLinksArgs = {
  pageNumber?: InputMaybe<Scalars['Int']['input']>;
  pageSize?: InputMaybe<Scalars['Int']['input']>;
};


export type QueryGetOpenProductLocatieArgs = {
  id: Scalars['UUID']['input'];
};


export type QueryGetOpenProductLocatiesArgs = {
  naam?: InputMaybe<Scalars['String']['input']>;
  pageNumber?: InputMaybe<Scalars['Int']['input']>;
  pageSize?: InputMaybe<Scalars['Int']['input']>;
};


export type QueryGetOpenProductOrganisatieArgs = {
  id: Scalars['UUID']['input'];
};


export type QueryGetOpenProductOrganisatiesArgs = {
  naam?: InputMaybe<Scalars['String']['input']>;
  pageNumber?: InputMaybe<Scalars['Int']['input']>;
  pageSize?: InputMaybe<Scalars['Int']['input']>;
};


export type QueryGetOpenProductPrijsArgs = {
  id: Scalars['UUID']['input'];
};


export type QueryGetOpenProductPrijzenArgs = {
  pageNumber?: InputMaybe<Scalars['Int']['input']>;
  pageSize?: InputMaybe<Scalars['Int']['input']>;
};


export type QueryGetOpenProductThemaArgs = {
  id: Scalars['UUID']['input'];
};


export type QueryGetOpenProductThemaHierarchyArgs = {
  id: Scalars['UUID']['input'];
};


export type QueryGetOpenProductThemaTakenArgs = {
  id: Scalars['UUID']['input'];
  language?: InputMaybe<Scalars['String']['input']>;
  pageSize?: InputMaybe<Scalars['Int']['input']>;
};


export type QueryGetOpenProductThemaZakenArgs = {
  id: Scalars['UUID']['input'];
  isOpen?: InputMaybe<Scalars['Boolean']['input']>;
  language?: InputMaybe<Scalars['String']['input']>;
  pageSize?: InputMaybe<Scalars['Int']['input']>;
};


export type QueryGetOpenProductThemasArgs = {
  pageNumber?: InputMaybe<Scalars['Int']['input']>;
  pageSize?: InputMaybe<Scalars['Int']['input']>;
};


export type QueryGetOpenProductTypeArgs = {
  id: Scalars['UUID']['input'];
  language?: InputMaybe<Scalars['String']['input']>;
};


export type QueryGetOpenProductTypesArgs = {
  language?: InputMaybe<Scalars['String']['input']>;
  pageNumber?: InputMaybe<Scalars['Int']['input']>;
  pageSize?: InputMaybe<Scalars['Int']['input']>;
};


export type QueryGetOpenProductenArgs = {
  pageNumber?: InputMaybe<Scalars['Int']['input']>;
  pageSize?: InputMaybe<Scalars['Int']['input']>;
  productTypeCode?: InputMaybe<Scalars['String']['input']>;
  productTypeCodes?: InputMaybe<Array<InputMaybe<Scalars['String']['input']>>>;
  productTypeId?: InputMaybe<Scalars['String']['input']>;
  productTypeIds?: InputMaybe<Array<InputMaybe<Scalars['String']['input']>>>;
  status?: InputMaybe<Scalars['String']['input']>;
};


export type QueryGetOpenProductenByThemaArgs = {
  themaId: Scalars['UUID']['input'];
};


export type QueryGetProductArgs = {
  id: Scalars['UUID']['input'];
};


export type QueryGetProductDecisionArgs = {
  dmnVariables?: InputMaybe<Scalars['JSON']['input']>;
  key: Scalars['String']['input'];
  productName: Scalars['String']['input'];
  productTypeId?: InputMaybe<Scalars['UUID']['input']>;
  sources?: InputMaybe<Scalars['JSON']['input']>;
};


export type QueryGetProductTakenArgs = {
  pageSize?: InputMaybe<Scalars['Int']['input']>;
  productName: Scalars['String']['input'];
  productSubType?: InputMaybe<Scalars['String']['input']>;
  productTypeId?: InputMaybe<Scalars['UUID']['input']>;
};


export type QueryGetProductTypeArgs = {
  productName: Scalars['String']['input'];
  productTypeId?: InputMaybe<Scalars['UUID']['input']>;
};


export type QueryGetProductVerbruiksObjectenArgs = {
  productId: Scalars['UUID']['input'];
};


export type QueryGetProductZakenArgs = {
  isOpen?: InputMaybe<Scalars['Boolean']['input']>;
  pageSize?: InputMaybe<Scalars['Int']['input']>;
  productName: Scalars['String']['input'];
  productTypeId?: InputMaybe<Scalars['UUID']['input']>;
};


export type QueryGetProductenArgs = {
  pageNumber?: InputMaybe<Scalars['Int']['input']>;
  pageSize?: InputMaybe<Scalars['Int']['input']>;
  productName: Scalars['String']['input'];
  productTypeId?: InputMaybe<Scalars['UUID']['input']>;
  subProductType?: InputMaybe<Scalars['String']['input']>;
};


export type QueryGetTaakByIdV2Args = {
  id: Scalars['UUID']['input'];
};


export type QueryGetTakenV2Args = {
  pageNumber?: InputMaybe<Scalars['Int']['input']>;
  pageSize?: InputMaybe<Scalars['Int']['input']>;
  status?: InputMaybe<TaakStatus>;
  title?: InputMaybe<Scalars['String']['input']>;
  zaakUUID?: InputMaybe<Scalars['UUID']['input']>;
};


export type QueryGetUserDigitaleAdressenArgs = {
  isStandaardAdres?: InputMaybe<Scalars['Boolean']['input']>;
};


export type QueryGetUserKlantContactArgs = {
  identificatorId?: InputMaybe<Scalars['UUID']['input']>;
  identificatorType?: InputMaybe<OnderwerpObjectIndentificatorType>;
  klantContactId: Scalars['UUID']['input'];
};


export type QueryGetUserKlantContactenArgs = {
  identificatorId?: InputMaybe<Scalars['UUID']['input']>;
  identificatorType?: InputMaybe<OnderwerpObjectIndentificatorType>;
};


export type QueryGetUserPartijArgs = {
  partijId: Scalars['UUID']['input'];
};


export type QueryGetZaakArgs = {
  id: Scalars['UUID']['input'];
};


export type QueryGetZakenArgs = {
  identificatie?: InputMaybe<Scalars['String']['input']>;
  identificatieContains?: InputMaybe<Scalars['String']['input']>;
  isOpen?: InputMaybe<Scalars['Boolean']['input']>;
  omschrijving?: InputMaybe<Scalars['String']['input']>;
  page?: InputMaybe<Scalars['Int']['input']>;
  pageSize?: InputMaybe<Scalars['Int']['input']>;
  zaakTypeUrl?: InputMaybe<Scalars['String']['input']>;
};


export type QueryProductPrefillArgs = {
  key: Scalars['String']['input'];
  productName: Scalars['String']['input'];
  productTypeId?: InputMaybe<Scalars['UUID']['input']>;
  sources?: InputMaybe<Scalars['JSON']['input']>;
  staticData?: InputMaybe<Scalars['JSON']['input']>;
};

export type ResultaatType = {
  __typename?: 'ResultaatType';
  omschrijving?: Maybe<Scalars['String']['output']>;
  omschrijvingGeneriek?: Maybe<Scalars['String']['output']>;
  resultaattypeomschrijving: Scalars['String']['output'];
  selectielijstklasse: Scalars['String']['output'];
  toelichting?: Maybe<Scalars['String']['output']>;
  url: Scalars['String']['output'];
  zaaktype: Scalars['String']['output'];
  zaaktypeIdentificatie?: Maybe<Scalars['String']['output']>;
};

export type SbiActiviteit = {
  __typename?: 'SbiActiviteit';
  indHoofdactiviteit: Scalars['String']['output'];
  sbiCode: Scalars['String']['output'];
  sbiOmschrijving: Scalars['String']['output'];
};

export enum SoortPartij {
  Contactpersoon = 'CONTACTPERSOON',
  Organisatie = 'ORGANISATIE',
  Persoon = 'PERSOON'
}

export enum Sort {
  Asc = 'ASC',
  Desc = 'DESC'
}

export type StatusType = {
  __typename?: 'StatusType';
  isEindstatus?: Maybe<Scalars['Boolean']['output']>;
  omschrijving: Scalars['String']['output'];
  omschrijvingGeneriek?: Maybe<Scalars['String']['output']>;
};

export type TaakForm = {
  __typename?: 'TaakForm';
  data?: Maybe<Scalars['JSON']['output']>;
  formulier: TaakFormulierV2;
};

export type TaakFormulierV2 = {
  __typename?: 'TaakFormulierV2';
  soort: Scalars['String']['output'];
  value: Scalars['String']['output'];
};

export type TaakIdentificatie = {
  __typename?: 'TaakIdentificatie';
  type: Scalars['String']['output'];
  value: Scalars['String']['output'];
};

export type TaakKoppeling = {
  __typename?: 'TaakKoppeling';
  registratie: Scalars['String']['output'];
  value?: Maybe<Scalars['String']['output']>;
};

export type TaakPageV2 = {
  __typename?: 'TaakPageV2';
  /**  The elements on this page */
  content: Array<TaakV2>;
  /**  The requested page number */
  number: Scalars['Int']['output'];
  /**  The number of elements on this page */
  numberOfElements: Scalars['Int']['output'];
  /**  The requested page size */
  size: Scalars['Int']['output'];
  /**  The total number of elements */
  totalElements: Scalars['Int']['output'];
  /**  The total number of available pages */
  totalPages: Scalars['Int']['output'];
};

export enum TaakSoort {
  Ogonebetaling = 'OGONEBETALING',
  Portaalformulier = 'PORTAALFORMULIER',
  Url = 'URL'
}

export enum TaakStatus {
  Afgerond = 'AFGEROND',
  Gesloten = 'GESLOTEN',
  Ingediend = 'INGEDIEND',
  Open = 'OPEN',
  Verwerkt = 'VERWERKT'
}

export type TaakUrl = {
  __typename?: 'TaakUrl';
  uri: Scalars['String']['output'];
};

export type TaakV2 = {
  __typename?: 'TaakV2';
  eigenaar: Scalars['String']['output'];
  id: Scalars['UUID']['output'];
  identificatie: TaakIdentificatie;
  koppeling: TaakKoppeling;
  ogonebetaling?: Maybe<OgoneBetaling>;
  portaalformulier?: Maybe<TaakForm>;
  soort: TaakSoort;
  status: TaakStatus;
  titel: Scalars['String']['output'];
  url?: Maybe<TaakUrl>;
  verloopdatum?: Maybe<Scalars['ZonedDateTime']['output']>;
};

export type ThemasPage = {
  __typename?: 'ThemasPage';
  /**  The elements on this page */
  content: Array<OpenProductThema>;
  /**  The requested page number */
  number: Scalars['Int']['output'];
  /**  The number of elements on this page */
  numberOfElements: Scalars['Int']['output'];
  /**  The requested page size */
  size: Scalars['Int']['output'];
  /**  The total number of elements */
  totalElements: Scalars['Int']['output'];
  /**  The total number of available pages */
  totalPages: Scalars['Int']['output'];
};

export type UpdateProductRequestInput = {
  dataobject?: InputMaybe<Scalars['JSON']['input']>;
  uuid: Scalars['UUID']['input'];
  verbruiksobject?: InputMaybe<Scalars['JSON']['input']>;
};

export type VerificatieCreateInput = {
  type: VerificatieType;
  uuid?: InputMaybe<Scalars['UUID']['input']>;
  waarde: Scalars['String']['input'];
};

export type VerificatieCreateResponse = {
  __typename?: 'VerificatieCreateResponse';
  errorMessage?: Maybe<Scalars['String']['output']>;
  success: Scalars['Boolean']['output'];
  uuid?: Maybe<Scalars['UUID']['output']>;
};

export enum VerificatieType {
  Email = 'EMAIL',
  Overig = 'OVERIG',
  Telefoonnummer = 'TELEFOONNUMMER'
}

export type VerificatieVerifyInput = {
  code: Scalars['String']['input'];
  type: VerificatieType;
  uuid?: InputMaybe<Scalars['UUID']['input']>;
  waarde: Scalars['String']['input'];
};

export type VerificatieVerifyResponse = {
  __typename?: 'VerificatieVerifyResponse';
  errorMessage?: Maybe<Scalars['String']['output']>;
  uuid?: Maybe<Scalars['UUID']['output']>;
  verified: Scalars['Boolean']['output'];
  verifiedOn: Scalars['ZonedDateTime']['output'];
};

export type VerificationConfig = {
  __typename?: 'VerificationConfig';
  enabled?: Maybe<Scalars['Boolean']['output']>;
  typesNeedVerification?: Maybe<Array<VerificatieType>>;
};

export type Vestiging = {
  __typename?: 'Vestiging';
  adressen?: Maybe<Array<Maybe<Adres>>>;
  eersteHandelsnaam: Scalars['String']['output'];
  indCommercieleVestiging: Scalars['String']['output'];
  indHoofdvestiging: Scalars['String']['output'];
  kvkNummer: Scalars['String']['output'];
  totaalWerkzamePersonen: Scalars['Int']['output'];
  vestigingsnummer: Scalars['String']['output'];
};

export type Zaak = {
  __typename?: 'Zaak';
  besluiten: Array<Besluit>;
  documenten: Array<Document>;
  einddatum?: Maybe<Scalars['Date']['output']>;
  identificatie: Scalars['String']['output'];
  omschrijving: Scalars['String']['output'];
  resultaat?: Maybe<ZaakResultaat>;
  startdatum: Scalars['Date']['output'];
  status?: Maybe<ZaakStatus>;
  statusGeschiedenis: Array<ZaakStatus>;
  statussen: Array<StatusType>;
  url: Scalars['String']['output'];
  uuid: Scalars['UUID']['output'];
  zaakdetails: ZaakDetails;
  zaaktype: ZaakType;
};

export type ZaakDetails = {
  __typename?: 'ZaakDetails';
  data: Array<Scalars['JSON']['output']>;
  zaak: Scalars['String']['output'];
};

export type ZaakPage = {
  __typename?: 'ZaakPage';
  /**  The elements on this page */
  content: Array<Zaak>;
  /**  The requested page number */
  number: Scalars['Int']['output'];
  /**  The number of elements on this page */
  numberOfElements: Scalars['Int']['output'];
  /**  The requested page size */
  size: Scalars['Int']['output'];
  /**  The total number of elements */
  totalElements: Scalars['Int']['output'];
  /**  The total number of available pages */
  totalPages: Scalars['Int']['output'];
};

export type ZaakResultaat = {
  __typename?: 'ZaakResultaat';
  resultaattype: ResultaatType;
  toelichting?: Maybe<Scalars['String']['output']>;
  url: Scalars['String']['output'];
  uuid: Scalars['UUID']['output'];
  zaak: Scalars['String']['output'];
};

export type ZaakStatus = {
  __typename?: 'ZaakStatus';
  datumStatusGezet: Scalars['String']['output'];
  statustype: ZaakStatusType;
  substatussen: Array<ZaakSubStatus>;
  url: Scalars['String']['output'];
  uuid: Scalars['UUID']['output'];
  zaak: Scalars['String']['output'];
};

export type ZaakStatusType = {
  __typename?: 'ZaakStatusType';
  isEindstatus: Scalars['Boolean']['output'];
  omschrijving: Scalars['String']['output'];
  omschrijvingGeneriek?: Maybe<Scalars['String']['output']>;
};

export type ZaakSubStatus = {
  __typename?: 'ZaakSubStatus';
  doelgroep: ZaakSubStatusDoelgroep;
  omschrijving: Scalars['String']['output'];
  status?: Maybe<Scalars['String']['output']>;
  tijdstip: Scalars['String']['output'];
  uuid: Scalars['UUID']['output'];
  zaak: Scalars['String']['output'];
};

export enum ZaakSubStatusDoelgroep {
  Betrokkenen = 'BETROKKENEN',
  GeenDoelgroep = 'GEEN_DOELGROEP',
  Intern = 'INTERN'
}

export type ZaakType = {
  __typename?: 'ZaakType';
  identificatie: Scalars['String']['output'];
  omschrijving: Scalars['String']['output'];
  omschrijvingGeneriek?: Maybe<Scalars['String']['output']>;
};

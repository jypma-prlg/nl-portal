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
import { OidcContext } from "@nl-portal/nl-portal-authentication";
import { createContext, useContext, useEffect, useMemo } from "react";
import { getFullName } from "../utils/person-data";
import {
  BrpPersoon,
  GetUserDigitaleAdressenQuery,
  MaatschappelijkeActiviteit,
  GetBedrijfDocument,
  GetGemachtigdeV2Document,
  GetPersoonV2Document,
  GetUserDigitaleAdressenDocument,
} from "@nl-portal/nl-portal-api";
import { useLazyQuery, useQuery } from "@apollo/client/react";

export interface UserContextInterface {
  isLoading: boolean;
  isPersoon: boolean;
  isVolmacht: boolean;
  username: string;
  usernameVolmacht: string;
  persoon?: BrpPersoon;
  bedrijf?: MaatschappelijkeActiviteit;
  contact?: GetUserDigitaleAdressenQuery;
}

const UserContext = createContext<UserContextInterface>(
  {} as UserContextInterface,
);

export const UserProvider = ({ children }: { children: React.ReactNode }) => {
  const { decodedToken, authenticationMethods } = useContext(OidcContext);

  const authenticationMethod = decodedToken?.middel || "";

  const isCompany = useMemo(() => {
    if (!authenticationMethod) return false;
    return !!authenticationMethods?.company?.includes(authenticationMethod);
  }, [authenticationMethod, authenticationMethods]);

  const isProxy = useMemo(() => {
    if (!authenticationMethod) return false;
    return !!authenticationMethods?.proxy?.includes(authenticationMethod);
  }, [authenticationMethod, authenticationMethods]);

  const isPersoon = !isCompany;
  const isVolmacht = isProxy;

  const [loadPersoon, { loading: persoonLoading, data: persoonData }] =
    useLazyQuery(GetPersoonV2Document);
  const [loadBedrijf, { loading: bedrijfLoading, data: bedrijfData }] =
    useLazyQuery(GetBedrijfDocument);
  const [
    loadGemachtigde,
    { loading: gemachtigdeLoading, data: gemachtigdeData },
  ] = useLazyQuery(GetGemachtigdeV2Document);

  const { data: contactData, loading: contactLoading } = useQuery(
    GetUserDigitaleAdressenDocument,
  );

  const isLoading =
    persoonLoading || bedrijfLoading || gemachtigdeLoading || contactLoading;

  useEffect(() => {
    if (isCompany) {
      loadBedrijf({ variables: {} }).catch((error) => {
        console.error("Error fetching bedrijf:", error);
      });
    } else {
      loadPersoon({ variables: {} }).catch((error) => {
        console.error("Error fetching persoon:", error);
      });
    }

    if (isProxy) {
      loadGemachtigde({ variables: {} }).catch((error) => {
        console.error("Error fetching gemachtigde:", error);
      });
    }
  }, [isCompany, isProxy, loadBedrijf, loadPersoon, loadGemachtigde]);

  const persoonNaam = useMemo(() => {
    const p = persoonData?.getPersoonV2;
    return p?.naam ? getFullName(p.naam) : "";
  }, [persoonData]);

  const bedrijfNaam = useMemo(() => {
    return bedrijfData?.getBedrijf?.naam || "";
  }, [bedrijfData]);

  const gemachtigdeNaam = useMemo(() => {
    const g = gemachtigdeData?.getGemachtigdeV2;
    if (!g) return "";
    return (
      (g.persoon?.naam ? getFullName(g.persoon.naam) : g.bedrijf?.naam) || ""
    );
  }, [gemachtigdeData]);

  const username = useMemo(() => {
    if (isVolmacht) return gemachtigdeNaam;
    return isCompany ? bedrijfNaam : persoonNaam;
  }, [isVolmacht, gemachtigdeNaam, isCompany, bedrijfNaam, persoonNaam]);

  const usernameVolmacht = useMemo(() => {
    if (!isVolmacht) return "";
    return isCompany ? bedrijfNaam : persoonNaam;
  }, [isVolmacht, isCompany, bedrijfNaam, persoonNaam]);

  return (
    <UserContext.Provider
      value={{
        isLoading,
        isPersoon,
        isVolmacht,
        username,
        usernameVolmacht,
        persoon: persoonData?.getPersoonV2 as BrpPersoon | undefined,
        bedrijf: bedrijfData?.getBedrijf as
          MaatschappelijkeActiviteit | undefined,
        contact: contactData,
      }}
    >
      {children}
    </UserContext.Provider>
  );
};

export default UserContext;

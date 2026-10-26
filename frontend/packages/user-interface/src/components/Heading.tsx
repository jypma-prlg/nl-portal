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
import {
  Heading1,
  Heading1Props,
  Heading2,
  Heading2Props,
  Heading3,
  Heading3Props,
  Heading4,
  Heading4Props,
  Heading5,
  Heading5Props,
} from "@utrecht/component-library-react";
import classNames from "classnames";

type Headers = "h1" | "h2" | "h3" | "h4" | "h5";

type HeadingProps =
  Heading1Props | Heading2Props | Heading3Props | Heading4Props | Heading5Props;

type Props = HeadingProps & {
  as?: Headers;
  size?: Headers;
};

const Heading = ({ as = "h1", size = as, className, ...props }: Props) => {
  const classes = classNames(className, {
    [`utrecht-heading-${size.replace("h", "")}`]: as !== size,
  });

  if (as === "h1") return <Heading1 className={classes} {...props} />;
  if (as === "h2") return <Heading2 className={classes} {...props} />;
  if (as === "h3") return <Heading3 className={classes} {...props} />;
  if (as === "h4") return <Heading4 className={classes} {...props} />;
  if (as === "h5") return <Heading5 className={classes} {...props} />;
};

export default Heading;

import Link from "next/link";

import { buttonVariants } from "@/components/ui/button";
import { cn } from "@/lib/utils";

const sizeClassName = {
  sm: "h-8 gap-2 px-3 py-1.5 text-sm",
  md: "h-10 gap-[10px] px-[14px] py-2 text-xl",
  lg: "h-12 gap-3 px-4 py-2.5 text-2xl",
} as const;

type PrimaryLinkProps = Omit<React.ComponentProps<typeof Link>, "size"> & {
  size?: keyof typeof sizeClassName;
};

export function PrimaryLink({ className, size = "md", ...props }: PrimaryLinkProps) {
  return (
    <Link
      className={cn(buttonVariants({ variant: "default" }), sizeClassName[size], className)}
      {...props}
    />
  );
}

import Link from "next/link";
import { cva, type VariantProps } from "class-variance-authority";

import { cn } from "@/lib/utils";

const primaryLinkVariants = cva(
  "bg-primary text-primary-foreground hover:bg-primary/90 inline-flex w-fit items-center justify-center rounded-[8px] font-medium whitespace-nowrap transition-all",
  {
    variants: {
      size: {
        sm: "h-8 gap-2 px-3 py-1.5 text-sm",
        md: "h-10 gap-[10px] px-[14px] py-2 text-xl",
        lg: "h-12 gap-3 px-4 py-2.5 text-2xl",
      },
    },
    defaultVariants: {
      size: "md",
    },
  }
);

type PrimaryLinkProps = React.ComponentProps<typeof Link> &
  VariantProps<typeof primaryLinkVariants>;

export function PrimaryLink({ className, size, ...props }: PrimaryLinkProps) {
  return <Link className={cn(primaryLinkVariants({ size }), className)} {...props} />;
}

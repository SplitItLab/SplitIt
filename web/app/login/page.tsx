"use client";
import Image from "next/image";
import { useState } from "react";
import { useForm } from "react-hook-form";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { Field, FieldLabel, FieldError, FieldGroup } from "@/components/ui/field";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import { Alert, AlertDescription } from "@/components/ui/alert";
import { Spinner } from "@/components/ui/spinner";
import { zodResolver } from "@hookform/resolvers/zod";
import { login, LoginError, LoginInput, loginSchema } from "@/lib/auth";
import { Eye, EyeOff } from "lucide-react";

export default function LoginPage() {
  const router = useRouter();
  const [generalError, setGeneralError] = useState<string | null>(null);
  const [showPassword, setShowPassword] = useState(false);
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<LoginInput>({
    resolver: zodResolver(loginSchema),
  });

  const onSubmit = async (data: LoginInput) => {
    setGeneralError(null);
    try {
      await login(data);
      router.push("/dashboard");
    } catch (err) {
      if (err instanceof LoginError) {
        setGeneralError(err.message);
      }
    }
  };

  return (
    <div className="relative grid h-screen md:grid-cols-2">
      <div className="relative hidden h-screen overflow-hidden md:block">
        <Image src="/register-image.png" alt="" fill unoptimized className="object-cover" />
      </div>

      <div className="flex flex-col items-center justify-center gap-2 text-center">
        <div className="w-full max-w-sm">
          <h1 className="text-[32px] font-extrabold text-black">Bienvenido a SplitIt</h1>
          <p className="text-muted-foreground text-sm font-medium">
            Ingresa tus datos para continuar
          </p>
          <form className="flex flex-col gap-[10px]" onSubmit={handleSubmit(onSubmit)} noValidate>
            <FieldGroup>
              {generalError && (
                <Alert variant="destructive">
                  <AlertDescription>{generalError}</AlertDescription>
                </Alert>
              )}

              <Field data-invalid={!!errors.email}>
                <FieldLabel htmlFor="email">Email</FieldLabel>
                <Input
                  id="email"
                  placeholder="tu@email.com"
                  autoComplete="email"
                  type="email"
                  aria-invalid={!!errors.email}
                  {...register("email")}
                />
                <FieldError errors={[errors.email]} />
              </Field>

              <Field data-invalid={!!errors.password}>
                <FieldLabel htmlFor="password">Contraseña</FieldLabel>
                <div className="relative">
                  <Input
                    id="password"
                    placeholder="Tu contraseña"
                    type={showPassword ? "text" : "password"}
                    autoComplete="current-password"
                    aria-invalid={!!errors.password}
                    className="pr-10"
                    {...register("password")}
                  />
                  <button
                    type="button"
                    className="text-muted-foreground hover:text-foreground absolute top-1/2 right-3 -translate-y-1/2"
                    onClick={() => setShowPassword((prev) => !prev)}
                    aria-label={showPassword ? "Ocultar contraseña" : "Mostrar contraseña"}
                  >
                    {showPassword ? (
                      <EyeOff className="h-4 w-4" aria-hidden="true" />
                    ) : (
                      <Eye className="h-4 w-4" aria-hidden="true" />
                    )}
                  </button>
                </div>
                <FieldError errors={[errors.password]} />
              </Field>

              <Button type="submit" disabled={isSubmitting}>
                {isSubmitting ? <Spinner /> : "Ingresar"}
              </Button>
            </FieldGroup>
          </form>
          <p className="text-muted-foreground mt-3 text-center text-sm">
            ¿ No tenés cuenta ?
            <Link className="font medium text-primary hover:underline" href="/register">
              Registrate gratis
            </Link>
          </p>
        </div>
      </div>
    </div>
  );
}

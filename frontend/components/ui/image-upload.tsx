"use client";

import * as React from "react";
import { UploadCloud, Image as ImageIcon, X, RefreshCw } from "lucide-react";
import { Button } from "./button";
import { cn } from "@/lib/cn";

export interface ImageUploadProps {
  id?: string;
  label?: string;
  hint?: string;
  error?: string;
  required?: boolean;
  value?: string | null; // existing image URL if editing
  disabled?: boolean;
  onChange: (file: File | null) => void;
  onRemoveExisting?: () => void;
  className?: string;
}

const MAX_FILE_SIZE_BYTES = 5 * 1024 * 1024; // 5 MB
const ACCEPTED_TYPES = ["image/jpeg", "image/jpg", "image/png", "image/webp"];
const ACCEPTED_EXTENSIONS = [".jpg", ".jpeg", ".png", ".webp"];

function formatFileSize(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}

export function ImageUpload({
  id: providedId,
  label = "Product Image",
  hint = "PNG, JPG, JPEG, WEBP • Max 5 MB",
  error: serverError,
  required = false,
  value,
  disabled = false,
  onChange,
  onRemoveExisting,
  className,
}: ImageUploadProps) {
  const generatedId = React.useId();
  const inputId = providedId ?? generatedId;
  const fileInputRef = React.useRef<HTMLInputElement | null>(null);

  const [selectedFile, setSelectedFile] = React.useState<File | null>(null);
  const [filePreviewUrl, setFilePreviewUrl] = React.useState<string | null>(null);
  const [isRemoved, setIsRemoved] = React.useState(false);
  const [localError, setLocalError] = React.useState<string | null>(null);
  const [isDragOver, setIsDragOver] = React.useState(false);

  // Clean up object URLs to avoid memory leaks
  React.useEffect(() => {
    return () => {
      if (filePreviewUrl && filePreviewUrl.startsWith("blob:")) {
        URL.revokeObjectURL(filePreviewUrl);
      }
    };
  }, [filePreviewUrl]);

  const validateAndProcessFile = React.useCallback(
    (file: File) => {
      setLocalError(null);
      setIsRemoved(false);

      // Validate MIME type & extension
      const fileExt = "." + (file.name.split(".").pop()?.toLowerCase() ?? "");
      const isAcceptedMime = file.type ? ACCEPTED_TYPES.includes(file.type.toLowerCase()) : false;
      const isAcceptedExt = ACCEPTED_EXTENSIONS.includes(fileExt);

      if (!isAcceptedMime && !isAcceptedExt) {
        setLocalError("Please upload a JPG, PNG, or WEBP image.");
        return false;
      }

      // Validate File Size
      if (file.size > MAX_FILE_SIZE_BYTES) {
        setLocalError("Image size must be less than 5 MB.");
        return false;
      }

      if (file.size === 0) {
        setLocalError("Selected file is empty.");
        return false;
      }

      const newPreviewUrl = URL.createObjectURL(file);
      if (filePreviewUrl && filePreviewUrl.startsWith("blob:")) {
        URL.revokeObjectURL(filePreviewUrl);
      }

      setSelectedFile(file);
      setFilePreviewUrl(newPreviewUrl);
      onChange(file);
      return true;
    },
    [filePreviewUrl, onChange]
  );

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const files = e.target.files;
    if (files && files.length > 0) {
      validateAndProcessFile(files[0]);
    }
  };

  const handleDragOver = (e: React.DragEvent<HTMLDivElement>) => {
    e.preventDefault();
    e.stopPropagation();
    if (!disabled) {
      setIsDragOver(true);
    }
  };

  const handleDragLeave = (e: React.DragEvent<HTMLDivElement>) => {
    e.preventDefault();
    e.stopPropagation();
    setIsDragOver(false);
  };

  const handleDrop = (e: React.DragEvent<HTMLDivElement>) => {
    e.preventDefault();
    e.stopPropagation();
    setIsDragOver(false);

    if (disabled) return;

    const files = e.dataTransfer.files;
    if (files && files.length > 0) {
      validateAndProcessFile(files[0]);
    }
  };

  const handleRemove = () => {
    if (filePreviewUrl && filePreviewUrl.startsWith("blob:")) {
      URL.revokeObjectURL(filePreviewUrl);
    }
    setSelectedFile(null);
    setFilePreviewUrl(null);
    setIsRemoved(true);
    setLocalError(null);
    if (fileInputRef.current) {
      fileInputRef.current.value = "";
    }
    onChange(null);
    if (onRemoveExisting) {
      onRemoveExisting();
    }
  };

  const handleBrowseClick = () => {
    if (!disabled && fileInputRef.current) {
      fileInputRef.current.click();
    }
  };

  const activeError = localError || serverError;
  const previewUrl = isRemoved ? null : (filePreviewUrl ?? (value ? value : null));

  return (
    <div className={cn("flex flex-col gap-2", className)}>
      <div className="flex items-center justify-between">
        <label htmlFor={inputId} className="text-[length:--text-small] font-medium text-ink">
          {label}
          {required && (
            <span className="ml-1 text-danger" aria-hidden="true">
              *
            </span>
          )}
          {required && <span className="sr-only"> (required)</span>}
        </label>
        {hint && <span className="text-[length:--text-caption] text-muted">{hint}</span>}
      </div>

      <input
        ref={fileInputRef}
        id={inputId}
        type="file"
        accept={ACCEPTED_EXTENSIONS.join(",")}
        onChange={handleFileChange}
        disabled={disabled}
        className="sr-only"
        aria-describedby={activeError ? `${inputId}-error` : undefined}
      />

      {previewUrl ? (
        /* Image Preview Box */
        <div className="flex flex-col gap-3 rounded-[--radius-md] border border-line bg-surface p-3 sm:flex-row sm:items-center sm:justify-between">
          <div className="flex items-center gap-3 min-w-0">
            <div className="relative size-16 shrink-0 overflow-hidden rounded-[--radius-sm] border border-line bg-sunken">
              {/* eslint-disable-next-line @next/next/no-img-element */}
              <img
                src={previewUrl}
                alt={selectedFile ? selectedFile.name : "Product image preview"}
                className="size-full object-cover"
              />
            </div>
            <div className="min-w-0 flex-1">
              <p className="truncate text-[length:--text-body] font-medium text-ink">
                {selectedFile ? selectedFile.name : "Current product image"}
              </p>
              <p className="text-[length:--text-caption] text-muted">
                {selectedFile ? formatFileSize(selectedFile.size) : "Saved on server"}
              </p>
            </div>
          </div>

          <div className="flex items-center gap-2 shrink-0">
            <Button
              type="button"
              variant="secondary"
              size="sm"
              onClick={handleBrowseClick}
              disabled={disabled}
              className="flex items-center gap-1.5"
            >
              <RefreshCw className="size-3.5" aria-hidden="true" />
              Change Image
            </Button>
            <Button
              type="button"
              variant="ghost"
              size="sm"
              onClick={handleRemove}
              disabled={disabled}
              className="flex items-center gap-1.5 text-danger hover:bg-danger-soft hover:text-danger"
            >
              <X className="size-3.5" aria-hidden="true" />
              Remove
            </Button>
          </div>
        </div>
      ) : (
        /* Dropzone Upload Area */
        <div
          onDragOver={handleDragOver}
          onDragLeave={handleDragLeave}
          onDrop={handleDrop}
          onClick={handleBrowseClick}
          onKeyDown={(e) => {
            if (e.key === "Enter" || e.key === " ") {
              e.preventDefault();
              handleBrowseClick();
            }
          }}
          role="button"
          tabIndex={disabled ? -1 : 0}
          aria-label="Upload product image: click to browse or drag and drop an image file"
          className={cn(
            "flex flex-col items-center justify-center gap-2 rounded-[--radius-md] border-2 border-dashed p-6 text-center cursor-pointer transition-colors duration-[--duration-fast] focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ink focus-visible:ring-offset-2",
            isDragOver
              ? "border-ink bg-sunken"
              : "border-line-strong bg-surface hover:border-ink/70 hover:bg-sunken/40",
            disabled && "cursor-not-allowed opacity-60 bg-sunken",
            activeError && "border-danger/70 bg-danger-soft/20"
          )}
        >
          <div className="flex size-11 items-center justify-center rounded-full bg-sunken text-ink">
            {isDragOver ? (
              <UploadCloud className="size-6 text-ink animate-pulse" aria-hidden="true" />
            ) : (
              <ImageIcon className="size-6 text-muted" aria-hidden="true" />
            )}
          </div>

          <div className="flex flex-col gap-0.5">
            <p className="text-[length:--text-body] font-medium text-ink">
              <span className="underline underline-offset-2">Click to browse</span> or drag and drop
            </p>
            <p className="text-[length:--text-caption] text-muted">
              PNG, JPG, JPEG, WEBP up to 5 MB
            </p>
          </div>
        </div>
      )}

      {activeError && (
        <p id={`${inputId}-error`} role="alert" className="text-[length:--text-caption] text-danger">
          {activeError}
        </p>
      )}
    </div>
  );
}

import { render, screen, fireEvent } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi, beforeEach } from "vitest";
import { ImageUpload } from "@/components/ui/image-upload";

describe("ImageUpload", () => {
  beforeEach(() => {
    // Mock URL.createObjectURL and URL.revokeObjectURL in jsdom
    if (!global.URL.createObjectURL) {
      global.URL.createObjectURL = vi.fn(() => "blob:mock-preview-url");
    }
    if (!global.URL.revokeObjectURL) {
      global.URL.revokeObjectURL = vi.fn();
    }
  });

  it("renders upload dropzone when no image is selected", () => {
    render(<ImageUpload label="Product Image" onChange={vi.fn()} />);

    expect(screen.getByText("Product Image")).toBeInTheDocument();
    expect(screen.getByText(/Click to browse/i)).toBeInTheDocument();
    expect(screen.getByText(/PNG, JPG, JPEG, WEBP up to 5 MB/i)).toBeInTheDocument();
  });

  it("accepts a valid JPG file via browse and displays preview", async () => {
    const user = userEvent.setup();
    const onChange = vi.fn();

    const { container } = render(<ImageUpload label="Product Image" onChange={onChange} />);

    const file = new File(["dummy content"], "watch.jpg", { type: "image/jpeg" });
    const input = container.querySelector('input[type="file"]') as HTMLInputElement;

    await user.upload(input, file);

    expect(onChange).toHaveBeenCalledWith(file);
    expect(screen.getByText("watch.jpg")).toBeInTheDocument();
    expect(screen.getByText("Change Image")).toBeInTheDocument();
    expect(screen.getByText("Remove")).toBeInTheDocument();
  });

  it("rejects unsupported file type with error message", async () => {
    const onChange = vi.fn();

    const { container } = render(<ImageUpload label="Product Image" onChange={onChange} />);

    const file = new File(["dummy script"], "script.exe", { type: "application/x-msdownload" });
    const input = container.querySelector('input[type="file"]') as HTMLInputElement;

    fireEvent.change(input, { target: { files: [file] } });

    expect(onChange).not.toHaveBeenCalled();
    expect(screen.getByRole("alert")).toHaveTextContent("Please upload a JPG, PNG, or WEBP image.");
  });

  it("rejects file larger than 5 MB with error message", async () => {
    const onChange = vi.fn();

    const { container } = render(<ImageUpload label="Product Image" onChange={onChange} />);

    // Create a 6MB dummy file
    const largeFile = new File([new Uint8Array(6 * 1024 * 1024)], "huge.png", { type: "image/png" });
    const input = container.querySelector('input[type="file"]') as HTMLInputElement;

    fireEvent.change(input, { target: { files: [largeFile] } });

    expect(onChange).not.toHaveBeenCalled();
    expect(screen.getByRole("alert")).toHaveTextContent("Image size must be less than 5 MB.");
  });

  it("clears selection when Remove button is clicked", async () => {
    const user = userEvent.setup();
    const onChange = vi.fn();
    const onRemoveExisting = vi.fn();

    render(
      <ImageUpload
        label="Product Image"
        value="/uploads/products/existing.webp"
        onChange={onChange}
        onRemoveExisting={onRemoveExisting}
      />
    );

    expect(screen.getByText("Current product image")).toBeInTheDocument();

    const removeBtn = screen.getByText("Remove");
    await user.click(removeBtn);

    expect(onRemoveExisting).toHaveBeenCalled();
    expect(onChange).toHaveBeenCalledWith(null);
    expect(screen.getByText(/Click to browse/i)).toBeInTheDocument();
  });
});

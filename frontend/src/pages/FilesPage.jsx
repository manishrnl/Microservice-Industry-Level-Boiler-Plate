import {Download, Eye, FileUp, Trash2} from "lucide-react";
import {useMutation, useQuery, useQueryClient} from "@tanstack/react-query";
import {apiClient} from "../api/axiosInstance";
import {endpoints} from "../api/endpoints";
import {PageWrapper} from "../components/common/PageWrapper";
import {asArray} from "../utils/responseUtils";

const contentTypesByExtension = {
    txt: "text/plain",
    text: "text/plain",
    md: "text/markdown",
    markdown: "text/markdown",
    pdf: "application/pdf",
    png: "image/png",
    jpg: "image/jpeg",
    jpeg: "image/jpeg",
    gif: "image/gif",
    webp: "image/webp",
    svg: "image/svg+xml",
    bmp: "image/bmp",
    ico: "image/x-icon",
    csv: "text/csv",
    json: "application/json",
    xml: "application/xml",
    html: "text/html",
    htm: "text/html",
    css: "text/css",
    js: "text/javascript",
    mp4: "video/mp4",
    webm: "video/webm",
    mp3: "audio/mpeg",
    wav: "audio/wav",
    zip: "application/zip",
    "7z": "application/x-7z-compressed",
    rar: "application/vnd.rar",
    tar: "application/x-tar",
    gz: "application/gzip",
    doc: "application/msword",
    docx: "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
    xls: "application/vnd.ms-excel",
    xlsx: "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
    ppt: "application/vnd.ms-powerpoint",
    pptx: "application/vnd.openxmlformats-officedocument.presentationml.presentation"
};

const extensionOf = (filename = "") => {
    const dot = filename.lastIndexOf(".");
    return dot >= 0 && dot < filename.length - 1 ? filename.slice(dot + 1).toLowerCase() : "";
};

const contentTypeForFile = (file, responseType) => {
    const extensionType = contentTypesByExtension[extensionOf(file.originalName)];
    if (extensionType) {
        return extensionType;
    }
    return responseType || file.contentType || "application/octet-stream";
};

const openBlob = (blob, filename, openInNewTab = false, openedWindow = null) => {
    const url = window.URL.createObjectURL(blob);
    if (openInNewTab) {
        if (openedWindow) {
            openedWindow.location.href = url;
        } else {
            window.open(url, "_blank", "noopener,noreferrer");
        }
        window.setTimeout(() => window.URL.revokeObjectURL(url), 60_000);
        return;
    }
    const anchor = document.createElement("a");
    anchor.href = url;
    anchor.download = filename || "download";
    document.body.appendChild(anchor);
    anchor.click();
    anchor.remove();
    window.setTimeout(() => window.URL.revokeObjectURL(url), 1_000);
};

const FilesPage = () => {
    const queryClient = useQueryClient();
    const files = useQuery({
        queryKey: ["files"],
        queryFn: async () => asArray((await apiClient.get(endpoints.files.mine)).data)
    });
    const upload = useMutation({
        mutationFn: async (file) => {
            const body = new FormData();
            body.append("file", file);
            return apiClient.post(endpoints.files.upload, body, {headers: {"Content-Type": "multipart/form-data"}});
        },
        onSuccess: () => queryClient.invalidateQueries({queryKey: ["files"]})
    });
    const remove = useMutation({
        mutationFn: async (id) => apiClient.delete(endpoints.files.delete(id)),
        onSuccess: () => queryClient.invalidateQueries({queryKey: ["files"]})
    });
    const fileBlob = async (file, inline = false) => {
        const response = await apiClient.get(inline ? endpoints.files.view(file.id) : endpoints.files.download(file.id), {
            responseType: "blob"
        });
        return new Blob([response.data], {type: contentTypeForFile(file, response.data?.type)});
    };
    const viewFile = async (file) => {
        const openedWindow = window.open("about:blank", "_blank");
        if (openedWindow) {
            openedWindow.opener = null;
        }
        try {
            const blob = await fileBlob(file, true);
            openBlob(blob, file.originalName, true, openedWindow);
        } catch (error) {
            openedWindow?.close();
            throw error;
        }
    };
    const download = async (file) => {
        const blob = await fileBlob(file);
        openBlob(blob, file.originalName);
    };
    return <PageWrapper title="Files">
        <div className="rounded-md border border-slate-200 bg-white">
            <label
                className="flex cursor-pointer items-center justify-between border-b px-4 py-3 hover:bg-slate-50"
            >
          <span className="inline-flex items-center gap-2 text-sm font-medium">
            <FileUp className="h-4 w-4"/>
            Upload file
          </span>
                <input
                    type="file"
                    className="hidden"
                    onChange={(event) => {
                        if (event.target.files?.[0]) {
                            upload.mutate(event.target.files[0]);
                            event.target.value = "";
                        }
                    }}
                />
            </label>
            {(files.data ?? []).length === 0 ?
                <div className="p-8 text-sm text-slate-500">No files returned by the
                    backend.</div> : files.data?.map((file) => <div
                    key={file.id}
                    className="grid grid-cols-[1fr_auto_auto_auto] items-center gap-3 border-b px-4 py-4 text-sm last:border-b-0"
                >
                    <div className="min-w-0">
                        <p className="font-medium text-slate-950">{file.originalName}</p>
                        <p className="text-xs text-slate-500">{file.contentType} · {file.sizeBytes} bytes</p>
                    </div>
                    <button
                        onClick={() => void viewFile(file)}
                        className="grid h-9 w-9 place-items-center rounded-md border border-slate-200 hover:bg-slate-50"
                        aria-label="View file"
                        title="View file"
                    >
                        <Eye className="h-4 w-4"/>
                    </button>
                    <button
                        onClick={() => void download(file)}
                        className="grid h-9 w-9 place-items-center rounded-md border border-slate-200 hover:bg-slate-50"
                        aria-label="Download file"
                        title="Download file"
                    >
                        <Download className="h-4 w-4"/>
                    </button>
                    <button
                        onClick={() => remove.mutate(file.id)}
                        className="grid h-9 w-9 place-items-center rounded-md border border-slate-200 text-slate-500 hover:bg-red-50 hover:text-red-600"
                        aria-label="Delete file"
                    >
                        <Trash2 className="h-4 w-4"/>
                    </button>
                </div>)}
        </div>
    </PageWrapper>;
};
export {
    FilesPage
};

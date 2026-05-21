import {Download, FileUp, Trash2} from "lucide-react";
import {useMutation, useQuery, useQueryClient} from "@tanstack/react-query";
import {apiClient} from "../api/axiosInstance";
import {endpoints} from "../api/endpoints";
import {PageWrapper} from "../components/common/PageWrapper";
import {asArray, unwrapApiData} from "../utils/responseUtils";

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
    const download = async (id) => {
        const response = await apiClient.get(endpoints.files.downloadUrl(id));
        const data = unwrapApiData(response.data);
        if (data.url) {
            window.open(data.url, "_blank", "noopener,noreferrer");
        }
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
                    onChange={(event) => event.target.files?.[0] && upload.mutate(event.target.files[0])}
                />
            </label>
            {(files.data ?? []).length === 0 ?
                <div className="p-8 text-sm text-slate-500">No files returned by the
                    backend.</div> : files.data?.map((file) => <div
                    key={file.id}
                    className="grid grid-cols-[1fr_auto_auto] items-center gap-3 border-b px-4 py-4 text-sm last:border-b-0"
                >
                    <div className="min-w-0">
                        <p className="font-medium text-slate-950">{file.originalName}</p>
                        <p className="text-xs text-slate-500">{file.contentType} · {file.sizeBytes} bytes</p>
                    </div>
                    <button
                        onClick={() => void download(file.id)}
                        className="grid h-9 w-9 place-items-center rounded-md border border-slate-200 hover:bg-slate-50"
                        aria-label="Download file"
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

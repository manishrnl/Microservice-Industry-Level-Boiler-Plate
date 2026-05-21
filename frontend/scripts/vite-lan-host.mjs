import {spawn} from "node:child_process";
import {fileURLToPath} from "node:url";
import path from "node:path";

const args = process.argv.slice(2);
const hasHostArg = args.some((arg, index) => arg === "--host" || arg.startsWith("--host=") || arg === "-H" || args[index - 1] === "--host" || args[index - 1] === "-H");
const explicitHost = process.env.VITE_DEV_HOST?.trim();

const scriptPath = path.dirname(fileURLToPath(import.meta.url));
const viteBin = path.resolve(scriptPath, "../node_modules/vite/bin/vite.js");
const viteArgs = hasHostArg ? args : ["--host", explicitHost || "0.0.0.0", ...args];

const child = spawn(process.execPath, [viteBin, ...viteArgs], {
    stdio: "inherit",
    shell: false
});

child.on("exit", (code, signal) => {
    if (signal) {
        process.kill(process.pid, signal);
        return;
    }
    process.exit(code ?? 0);
});

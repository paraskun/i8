const base = 'http://localhost:8080';

export namespace Plugin {
    export type Manifest = {
        name: string;
        type: string;
        dist: string;
        description: string;
        withStyle: boolean;
    };

    export interface Instance {
        mount(options?: any): void;
        unmount(options?: any): void;
    };

    export type Contructor = {
        new(): Instance;
    };

    async function load(manifest: Manifest): Promise<Contructor> {
        if (manifest.withStyle) {
            const link = document.createElement("link");

            link.rel = "stylesheet";
            link.href = base + manifest.dist + "/plugin.css";

            document.head.appendChild(link);
        }

        return await import(/* @vite-ignore */ `${base}/${manifest.dist}/plugin.js`).then((node) => node.default);
    }

    let manifests: Record<string, Manifest> = {};
    let plugins: Record<string, Contructor> = {};

    export async function list(): Promise<Record<string, Manifest>> {
        if (Object.entries(manifests).length == 0) {
            manifests = await fetch(base + '/plug')
                .then(response => response.json())
                .then((data: Manifest[]) => {
                    let result: Record<string, Manifest> = {};

                    for (let manifest of data) {
                        result[manifest.type] = manifest;
                    }

                    return result;
                })
                .catch((error) => {
                    console.error('Error:', error);
                    return {};
                });
        }

        return manifests;
    }

    export async function get(manifest: Manifest): Promise<Contructor> {
        if (manifest.type in plugins) {
            return plugins[manifest.type];
        }

        plugins[manifest.type] = await load(manifest);

        return plugins[manifest.type];
    }
};


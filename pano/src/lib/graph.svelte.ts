import { type Connection } from "@xyflow/svelte";
import { Plugin } from "./plugin.svelte";
import { tick } from "svelte";

export type Port = {
    type: string;
    schema: string | undefined;
};

export type Position = {
    x: number;
    y: number;
};

export type NodeMeta = {
    id: number;
    type: string;
    position: Position;
    configuration: any;
    ports: Port[];
};

export class Node {
    id: number;
    manifest: Plugin.Manifest;
    position: Position;
    configuration: any = $state({});
    ports: Port[] = $state([]);

    constructor(
        id: number,
        manifest: Plugin.Manifest,
        position: Position = { x: 100, y: 100 },
        configuration: any = {},
        ports: Port[] = []
    ) {
        this.id = id;
        this.manifest = manifest;
        this.position = position;
        this.configuration = configuration;
        this.ports = this.ports.concat(ports);
    }

    sync() {
        graph.dirty = true;
    }

    async update(data: any) {
        if (!$state.snapshot(graph.dirty)) {
            await fetch(`http://localhost:8080/node/${this.id}`, {
                method: "PUT",
                headers: {
                    mode: "cors",
                    "Access-Control-Allow-Origin": "*",
                    "Access-Control-Request-Method": "PUT",
                    "Access-Control-Request-Headers": "Content-Type",
                    "Content-Type": "application/json",
                },
                body: JSON.stringify(data),
            });
        }
    }

    async monitor(): Promise<any[] | null> {
        if (!$state.snapshot(graph.dirty)) {
            return await fetch(`http://localhost:8080/node/${this.id}`, {
                method: "GET",
                headers: {
                    mode: "cors",
                    "Access-Control-Allow-Origin": "*",
                    "Access-Control-Request-Method": "GET",
                },
            }).then((response) => response.json());
        }

        return Promise.resolve(null);
    }

    meta(): NodeMeta {
        return {
            id: this.id,
            type: this.manifest.type,
            position: this.position,
            configuration: $state.snapshot(this.configuration),
            ports: $state.snapshot(this.ports),
        };
    }
};

type End = {
    node: number;
    port: number;
};

type Link = {
    id: number;
    source: End;
    target: End;
};

class Graph {
    initialized = false;

    manifests: Record<string, Plugin.Manifest> | null = null;
    syncing = $state(false);

    dirty = $state(false);
    nodes = $state.raw<Node[]>([]);
    links = $state.raw<Link[]>([]);

    async sync(): Promise<boolean> {
        this.syncing = true;

        if (this.manifests == null) {
            this.manifests = await Plugin.list();
        }

        let body = JSON.stringify({
            nodes: null,
            links: null
        });

        if (this.initialized) {
            body = JSON.stringify({
                nodes: this.nodes.map(node => node.meta()),
                links: this.links.map(link => ({
                    source: link.source,
                    target: link.target,
                }))
            })
        }

        let meta = await fetch('http://localhost:8080/sync', {
            method: 'PUT',
            mode: 'cors',
            headers: {
                "Access-Control-Allow-Origin": "*",
                "Access-Control-Request-Method": "PUT",
                "Access-Control-Request-Headers": "Content-Type",
                'Content-Type': 'application/json'
            },
            body: body
        })
            .then(response => response.json())
            .then((data) => {
                return {
                    nodes: data.nodes as NodeMeta[],
                    links: data.links as Link[]
                }
            })
            .catch((error) => {
                return null;
            });

        if (meta == null) {
            return false;
        }

        this.nodes = meta.nodes.map((meta) => new Node(
            meta.id,
            this.manifests![meta.type],
            meta.position,
            meta.configuration,
            meta.ports
        ));

        this.links = meta.links.map((meta, i) => ({
            id: i,
            source: meta.source,
            target: meta.target
        }));

        await tick();

        this.syncing = false;
        this.dirty = false;

        if (!this.initialized) {
            this.initialized = true;
        }

        return true;
    }

    add(manifest: Plugin.Manifest) {
        this.nodes = this.nodes.concat(new Node(this.nodes.length, manifest));
        this.dirty = true;
    }

    onConnect(conn: Connection) {
        this.links = this.links.concat({
            id: this.links.length,
            source: {
                node: Number(conn.source),
                port: Number(conn.sourceHandle)
            },
            target: {
                node: Number(conn.target),
                port: Number(conn.targetHandle)
            }
        });

        this.dirty = true;
    }

    onDelete({ nodes, edges }: { nodes: any[], edges: any[] }) {
        if (nodes.length > 0) {
            for (let node of nodes) {
                this.nodes = this.nodes.filter(n => n.id != Number(node.id));
            }

            this.nodes = this.nodes.map((n, i) => {
                n.id = i;
                return n;
            })
        }

        if (edges.length > 0) {
            for (let edge of edges) {
                this.links = this.links.filter(l => l.id != Number(edge.id));
            }

            this.links = this.links.map((l, i) => {
                l.id = i;
                return l;
            });
        }

        this.dirty = true;
    }

    onNodeDrag(data: any) {
        for (const node of data.nodes) {
            this.nodes[Number(node.id)].position = node.position;
        }
    }

    onPortsChanged(node: number) {
        if (!this.syncing) {
            this.links = this.links
                .filter(l => l.source.node != node && l.target.node != node)
                .map((l, i) => {
                    l.id = i;
                    return l;

                });

            this.dirty = true;
        }
    }
};

export const graph = new Graph();
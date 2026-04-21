<script lang="ts">
    type Port = {
        type: string;
        schema: string | undefined;
    };

    interface Node {
        configuration: any;
        ports: Port[];

        sync(): void;
        update(data: any): void;
        monitor(): Promise<any[] | null>;
    }

    type Log = {
        text: string;
    };

    let { node }: { node: Node } = $props();

    node.ports = [
        {
            type: "target",
            schema: "DoubleValue",
        },
    ];

    let listen = $state(false);
    let process: Promise<void> = $state(null);
    let progress: Promise<void> = $state(null);

    async function start(): Promise<void> {
        await node.monitor().then((data) => {
            if (data == null) {
                return;
            }

            if (data.length > 0) {
                console.log(data as Log[]);
            }
        });

        if (listen) {
            return start();
        }

        return;
    }

    function toggle() {
        if (listen) {
            progress = process;
            listen = false;
        } else {
            listen = true;
            process = start();
        }
    }
</script>

<div class="flex flex-col gap-2">
    <h3>Printer</h3>
    {#await progress}
        <p>Wait...</p>
    {:then}
        <button class="btn btn-soft btn-primary" onclick={() => toggle()}>
            {#if listen}
                Stop
            {:else}
                Start
            {/if}
        </button>
    {/await}
</div>

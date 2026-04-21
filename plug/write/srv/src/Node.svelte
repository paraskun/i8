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

    let { node }: { node: Node } = $props();

    node.ports = [
        {
            type: "source",
            schema: "DoubleValue",
        },
    ];

    function write() {
        node.update({
            value: node.configuration.value,
        });
    }
</script>

<div class="flex flex-col gap-2">
    <h3>Writer....</h3>
    <input
        class="w-full input"
        type="number"
        step="0.01"
        bind:value={node.configuration.value}
    />
    <button class="w-full btn btn-soft btn-primary" onclick={() => write()}
        >Write</button
    >
</div>

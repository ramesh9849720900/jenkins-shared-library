package org.example

class PipelineUtils implements Serializable {
    def steps

    PipelineUtils(steps) {
        this.steps = steps
    }

    def sayHello() {
        steps.echo "Hello from PipelineUtils!"
    }
}

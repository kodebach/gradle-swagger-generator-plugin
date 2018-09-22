import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

import static Fixture.cleanBuildDir
import static Fixture.setupFixture

class CodeGeneratorOpenAPI3Spec extends Specification {
    GradleRunner runner
    def setup() {
        runner = GradleRunner.create()
            .withProjectDir(new File('code-generator-openapi3'))
            .withPluginClasspath()
            .forwardOutput()
        cleanBuildDir(runner)
    }

    def 'plugin should add default tasks into the project'() {
        given:
        runner.withArguments('--stacktrace', 'tasks')

        when:
        def result = runner.build()

        then:
        result.output.contains('generateSwaggerCode -')
        result.output.contains('generateSwaggerCodeHelp -')
    }

    def 'generateSwaggerCode task should generate code'() {
        def conditions = new PollingConditions(timeout: 10, initialDelay: 1.5, factor: 1.25)

        given:
        setupFixture(runner, Fixture.YAML.petstore_openapi3)
        runner.withArguments('--stacktrace', 'generateSwaggerCode')

        when:
        def result = runner.build()

        then:
        conditions.eventually {
            result.task(':generateSwaggerCode').outcome == TaskOutcome.NO_SOURCE
            result.task(':generateSwaggerCodePetstore').outcome == TaskOutcome.SUCCESS
            new File(runner.projectDir, 'build/swagger-code-petstore/src/main/java/example/api/PetsApi.java').exists()
        }

        when:
        def rerunResult = runner.build()

        then:
        rerunResult.task(':generateSwaggerCode').outcome == TaskOutcome.NO_SOURCE
        rerunResult.task(':generateSwaggerCodePetstore').outcome == TaskOutcome.UP_TO_DATE
    }

    def 'build task should generate and build code'() {
        def conditions = new PollingConditions(timeout: 10, initialDelay: 1.5, factor: 1.25)

        given:
        setupFixture(runner, Fixture.YAML.petstore_openapi3)
        runner.withArguments('--stacktrace', 'build')

        when:
        runner.build()

        then:
        conditions.eventually {
            new File(runner.projectDir, 'build/libs/code-generator.jar').exists()
        }
    }

    def 'generateSwaggerCodePetstoreHelp task should show help'() {
        given:
        runner.withArguments('--stacktrace', 'generateSwaggerCodePetstoreHelp')

        when:
        def result = runner.build()

        then:
        result.output.contains('CONFIG OPTIONS')
    }
}
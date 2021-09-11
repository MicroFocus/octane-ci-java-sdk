package com.hp.octane.integrations.dto.entities;

public class EntityConstants {
    public static class Errors{
        public static final String DUPLICATE_ERROR_CODE = "platform.duplicate_entity_error";
    }

    public static class Base {
        public static final String ID_FIELD = "id";
        public static final String NAME_FIELD = "name";
        public static final String LOGICAL_NAME_FIELD = "logical_name";
        public static final String DESCRIPTION_FIELD = "description";
        public static final String TYPE_FIELD_NAME = "type";
    }

    public static class Test extends Base {
        public static final String COLLECTION_NAME = "tests";
        public static final String ENTITY_NAME = "test";

    }
    public static class AutomatedTest extends Base {
        public static final String COLLECTION_NAME = "automated_tests";
        public static final String ENTITY_NAME = "automated_test";

        public static final String TEST_RUNNER_FIELD = "test_runner";
        public static final String SCM_REPOSITORY_FIELD = "scm_repository";
        public static final String TESTING_TOOL_TYPE_FIELD = "testing_tool_type";
        public static final String TEST_TYPE_FIELD = "test_type";
        public static final String FRAMEWORK_FIELD = "framework";
        public static final String PACKAGE_FIELD = "package";
        public static final String CLASS_NAME_FIELD = "class_name";
        public static final String EXECUTABLE_FIELD = "executable";
    }

    public static class MbtUnit extends Base {
        public static final String COLLECTION_NAME = "model_items";
        public static final String ENTITY_NAME = "model_item";
        public static final String ENTITY_SUBTYPE = "unit";

        public static final String SUBTYPE_FIELD = "subtype";
        public static final String PARENT = "parent";
        public static final String AUTOMATION_STATUS_FIELD = "automation_status";
        public static final String REPOSITORY_PATH_FIELD = "repository_path";
        public static final String TESTING_TOOL_TYPE_FIELD = "testing_tool_type";
    }

    public static class MbtUnitParameter extends Base {
        public static final String COLLECTION_NAME = "entity_parameters";
        public static final String ENTITY_NAME = "entity_parameter";
        public static final String ENTITY_SUBTYPE = "unit_parameter";

        public static final String SUBTYPE_FIELD = "subtype";
        public static final String MODEL_ITEM = "model_item";
        public static final String TYPE = "parameter_type";
        public static final String DEFAULT_VALUE = "value";
    }

    public static class ModelFolder extends Base {
        public static final String COLLECTION_NAME = "model_items";
        public static final String ENTITY_NAME = "model_item";
        public static final String ENTITY_SUBTYPE = "model_folder";

        public static final String LOGICAL_NAME = "logical_name";
    }

    public static class ScmResourceFile extends Base {
        public static final String COLLECTION_NAME = "scm_resource_files";
        public static final String ENTITY_NAME = "scm_resource_file";

        public static final String RELATIVE_PATH_FIELD = "relative_path";
        public static final String SCM_REPOSITORY_FIELD = "scm_repository";
    }

    public static class ScmRepository extends Base {
        public static final String COLLECTION_NAME = "scm_repositories";
        public static final String ENTITY_NAME = "scm_repository";

        @Deprecated
        public static final String BRANCH_FIELD = "branch";
        public static final String PARENT_FIELD = "repository";

        public static final String IS_MERGED_FIELD = "is_merged";
        public static final String IS_DELETED_FIELD = "is_deleted";
        public static final String LAST_COMMIT_SHA_FIELD = "last_commit_revision";
        public static final String LAST_COMMIT_TIME_FIELD = "last_commit_time";
        public static final String SCM_USER_FIELD = "scm_user";
        public static final String SCM_USER_EMAIL_FIELD = "scm_user_email";
    }

    public static class ScmRepositoryRoot extends Base {
        public static final String COLLECTION_NAME = "scm_repository_roots";
        public static final String ENTITY_NAME = "scm_repository_root";
        public static final String URL_FIELD = "url";
        public static final String SCM_TYPE_FIELD = "scm_type";
        public static final String SOURCE_VIEW_TEMPLATE ="scm_source_view";
        public static final String DIFF_TEMPLATE = "scm_diff";
        public static final String BRANCH_TEMPLATE ="scm_branch_file_link";
    }

    public static class Executors extends Base {
        public static final String COLLECTION_NAME = "executors";
		public static final String ENTITY_NAME = "executor";
        public static final String TEST_RUNNER_SUBTYPE_ENTITY_NAME = "test_runner";
        public static final String UFT_TEST_RUNNER_SUBTYPE_ENTITY_NAME = "uft_test_runner";
    }

    public static class CIServer extends Base {
        public static final String COLLECTION_NAME = "ci_servers";
        public static final String ENTITY_NAME = "ci_server";
        public static final String INSTANCE_ID_FIELD = "instance_id";
    }

    public static class Taxonomy extends Base {
        public static final String COLLECTION_NAME = "taxonomy_nodes";
        public static final String CATEGORY_NAME = "category";
    }

    public static class Release extends Base {
        public static final String COLLECTION_NAME = "releases";
        public static final String ENTITY_NAME = "release";
        public static final String IS_DEFAULT_FIELD = "is_default";
    }

    public static class TestSuiteLinkToTest extends Base {
        public static final String COLLECTION_NAME = "test_suite_link_to_tests";
        public static final String ENTITY_NAME = "test_suite_link_to_test";
        public static final String INCLUDE_IN_NEXT_RUN_FIELD = "include_in_next_run";
        public static final String TEST_SUITE_FIELD = "test_suite";
        public static final String RUN_MODE_FIELD = "run_mode";
        public static final String TEST_RUNNER_FIELD = "test_runner";
        public static final String EXECUTION_PARAMETERS_FIELD = "execution_parameters";

    }

    public static class Run extends Base {
        public static final String COLLECTION_NAME = "runs";
        public static final String RELEASE_FIELD = "release";
        public static final String SUBTYPE_FIELD = "subtype";
        public static final String TEST_FIELD = "test";
        public static final String NATIVE_STATUS_FIELD = "native_status";
    }

    public static class Milestone extends Base {
        public static final String COLLECTION_NAME = "milestones";
        public static final String RELEASE_FIELD = "release";
    }

    public static class Lists extends Base {
        public static final String COLLECTION_NAME = "list_nodes";
        public static final String ENTITY_NAME = "list_node";
    }

    public static class Workspaces extends Base {
        public static final String COLLECTION_NAME = "workspaces";
    }
}

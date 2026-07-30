<template>
  <el-dialog
    v-model="visible"
    :title="isEdit ? '编辑水果' : '新建水果'"
    width="520px"
    :close-on-click-modal="true"
    destroy-on-close
    @closed="handleClosed"
  >
    <el-form
      ref="formRef"
      :model="form"
      :rules="rules"
      label-width="90px"
      @keydown.esc="visible = false"
    >
      <el-form-item label="品类" prop="category">
        <el-select v-model="form.category" placeholder="请选择品类" clearable>
          <el-option
            v-for="item in CATEGORY_OPTIONS"
            :key="item"
            :label="item"
            :value="item"
          />
        </el-select>
      </el-form-item>

      <el-form-item label="品种名称" prop="variety">
        <el-input v-model="form.variety" placeholder="如：赣南脐橙" maxlength="30" show-word-limit />
      </el-form-item>

      <el-form-item label="等级" prop="grade">
        <el-select v-model="form.grade" placeholder="请选择等级" clearable>
          <el-option
            v-for="item in GRADE_OPTIONS"
            :key="item"
            :label="item"
            :value="item"
          />
        </el-select>
      </el-form-item>

      <el-form-item label="产地" prop="origin">
        <el-input v-model="form.origin" placeholder="如：江西赣州" maxlength="50" show-word-limit />
      </el-form-item>

      <el-form-item label="默认单位" prop="defaultUnit">
        <el-select v-model="form.defaultUnit" placeholder="请选择默认单位" clearable>
          <el-option label="克 (g)" value="g" />
          <el-option label="千克 (kg)" value="kg" />
          <el-option label="个" value="个" />
        </el-select>
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" :loading="submitting" @click="handleSubmit">
        {{ isEdit ? '保存' : '创建' }}
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, reactive, watch } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'

const CATEGORY_OPTIONS = ['柑橘类', '仁果类', '核果类', '浆果类', '热带水果', '瓜类']
const GRADE_OPTIONS = ['特级', '一级', '二级', '统货']

const props = defineProps<{
  modelValue: boolean
  editData?: CanonicalFruit | null
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', val: boolean): void
  (e: 'submit', dto: CreateFruitRequest): void
}>()

const visible = ref(false)
const submitting = ref(false)
const formRef = ref<FormInstance>()

const form = reactive<CreateFruitRequest>({
  category: '',
  variety: '',
  grade: '',
  origin: '',
  defaultUnit: '',
})

const isEdit = ref(false)

const rules: FormRules = {
  category: [{ required: true, message: '请选择品类', trigger: 'change' }],
  variety: [
    { required: true, message: '请输入品种名称', trigger: 'blur' },
    { min: 1, max: 30, message: '品种名称长度 1-30', trigger: 'blur' },
  ],
  grade: [{ required: true, message: '请选择等级', trigger: 'change' }],
  origin: [
    { required: true, message: '请输入产地', trigger: 'blur' },
    { max: 50, message: '产地最多 50 个字符', trigger: 'blur' },
  ],
  defaultUnit: [{ required: true, message: '请选择默认单位', trigger: 'change' }],
}

watch(
  () => props.modelValue,
  (val) => {
    visible.value = val
    if (val) {
      if (props.editData) {
        isEdit.value = true
        form.category = props.editData.category
        form.variety = props.editData.variety
        form.grade = props.editData.grade
        form.origin = props.editData.origin
        form.defaultUnit = props.editData.defaultUnit
      } else {
        isEdit.value = false
        resetForm()
      }
    }
  }
)

watch(visible, (val) => {
  emit('update:modelValue', val)
})

function resetForm() {
  form.category = ''
  form.variety = ''
  form.grade = ''
  form.origin = ''
  form.defaultUnit = ''
  formRef.value?.clearValidate()
}

function handleClosed() {
  resetForm()
}

async function handleSubmit() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    submitting.value = true
    try {
      emit('submit', { ...form })
      ElMessage.success(isEdit.value ? '更新成功' : '创建成功')
      visible.value = false
    } finally {
      submitting.value = false
    }
  })
}
</script>

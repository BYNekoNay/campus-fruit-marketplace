import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import type { App } from 'vue'
import { getAccessToken, hasRole, isMerchant } from '@/utils/auth'
import { ElMessage } from 'element-plus'

// Layouts (lazy-loaded)
const DefaultLayout = () => import('@/layouts/DefaultLayout.vue')
const MerchantLayout = () => import('@/layouts/MerchantLayout.vue')
const AdminLayout = () => import('@/layouts/AdminLayout.vue')

// Pages (lazy-loaded)
const HomePage = () => import('@/views/HomePage.vue')
const LoginPage = () => import('@/views/LoginPage.vue')
const RegisterPage = () => import('@/views/RegisterPage.vue')
const DiscoveryPage = () => import('@/views/DiscoveryPage.vue')
const StoreDetailPage = () => import('@/views/StoreDetailPage.vue')
const CartPage = () => import('@/views/CartPage.vue')
const OrderListPage = () => import('@/views/OrderListPage.vue')
const OrderDetailPage = () => import('@/views/OrderDetailPage.vue')
const AccountPage = () => import('@/views/AccountPage.vue')
const FavoritesPage = () => import('@/views/FavoritesPage.vue')
const NotFoundPage = () => import('@/views/NotFoundPage.vue')

// Merchant pages
const ApplyPage = () => import('@/views/merchant/ApplyPage.vue')
const MerchantDashboardPage = () => import('@/views/merchant/DashboardPage.vue')
const StoreListPage = () => import('@/views/merchant/StoreListPage.vue')
const StoreFormPage = () => import('@/views/merchant/StoreFormPage.vue')
const StaffManagePage = () => import('@/views/merchant/StaffManagePage.vue')

// Admin pages
const ReviewListPage = () => import('@/views/admin/merchant/ReviewListPage.vue')
const MerchantDetailPage = () => import('@/views/admin/merchant/MerchantDetailPage.vue')
const FruitListPage = () => import('@/views/admin/fruits/FruitListPage.vue')

// Merchant offer pages
const OfferListPage = () => import('@/views/merchant/offers/OfferListPage.vue')

// Merchant order pages
const MerchantOrderListPage = () => import('@/views/merchant/orders/MerchantOrderListPage.vue')
const MerchantOrderDetailPage = () => import('@/views/merchant/orders/MerchantOrderDetailPage.vue')

// Review pages
const ReviewSubmitPage = () => import('@/views/ReviewSubmitPage.vue')
const ReportListPage = () => import('@/views/admin/reviews/ReportListPage.vue')

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    component: DefaultLayout,
    children: [
      {
        path: '',
        name: 'Home',
        component: HomePage,
        meta: { title: '首页' },
      },
      {
        path: 'discovery',
        name: 'Discovery',
        component: DiscoveryPage,
        meta: { title: '发现' },
      },
      {
        path: 'stores/:id',
        name: 'StoreDetail',
        component: StoreDetailPage,
        props: true,
        meta: { title: '门店详情' },
      },
      {
        path: 'cart',
        name: 'Cart',
        component: CartPage,
        meta: { title: '购物车', requiresAuth: true },
      },
      {
        path: 'orders',
        name: 'Orders',
        component: OrderListPage,
        meta: { title: '我的订单', requiresAuth: true },
      },
      {
        path: 'orders/:id',
        name: 'OrderDetail',
        component: OrderDetailPage,
        props: true,
        meta: { title: '订单详情', requiresAuth: true },
      },
      {
        path: 'review/submit',
        name: 'ReviewSubmit',
        component: ReviewSubmitPage,
        meta: { title: '提交评价', requiresAuth: true },
      },
      {
        path: 'account',
        name: 'Account',
        component: AccountPage,
        meta: { title: '个人中心', requiresAuth: true },
      },
      {
        path: 'favorites',
        name: 'Favorites',
        component: FavoritesPage,
        meta: { title: '我的收藏', requiresAuth: true },
      },
    ],
  },
  {
    path: '/auth/login',
    name: 'Login',
    component: LoginPage,
    meta: { title: '登录', guest: true },
  },
  {
    path: '/auth/register',
    name: 'Register',
    component: RegisterPage,
    meta: { title: '注册', guest: true },
  },
  {
    path: '/merchant',
    component: MerchantLayout,
    meta: { requiresAuth: true, roles: ['ROLE_MERCHANT', 'ROLE_ADMIN'] },
    children: [
      {
        path: 'apply',
        name: 'MerchantApply',
        component: ApplyPage,
        meta: { title: '商家入驻', roles: ['ROLE_USER'] },
      },
      {
        path: 'dashboard',
        name: 'MerchantDashboard',
        component: MerchantDashboardPage,
        meta: { title: '商家工作台', roles: ['ROLE_MERCHANT', 'ROLE_ADMIN'] },
      },
      {
        path: 'stores',
        name: 'MerchantStores',
        component: StoreListPage,
        meta: { title: '门店管理', roles: ['ROLE_MERCHANT', 'ROLE_ADMIN'] },
      },
      {
        path: 'stores/create',
        name: 'MerchantStoreCreate',
        component: StoreFormPage,
        meta: { title: '新建门店', roles: ['ROLE_MERCHANT', 'ROLE_ADMIN'] },
      },
      {
        path: 'stores/:id/edit',
        name: 'MerchantStoreEdit',
        component: StoreFormPage,
        props: true,
        meta: { title: '编辑门店', roles: ['ROLE_MERCHANT', 'ROLE_ADMIN'] },
      },
      {
        path: 'stores/:id/staff',
        name: 'MerchantStoreStaff',
        component: StaffManagePage,
        props: true,
        meta: { title: '员工管理', roles: ['ROLE_MERCHANT', 'ROLE_ADMIN'] },
      },
      {
        path: 'offers',
        name: 'MerchantOffers',
        component: OfferListPage,
        meta: { title: '商品报价', roles: ['ROLE_MERCHANT', 'ROLE_ADMIN'] },
      },
      {
        path: 'orders',
        name: 'MerchantOrders',
        component: MerchantOrderListPage,
        meta: { title: '订单管理', roles: ['ROLE_MERCHANT', 'ROLE_ADMIN'] },
      },
      {
        path: 'orders/:id',
        name: 'MerchantOrderDetail',
        component: MerchantOrderDetailPage,
        props: true,
        meta: { title: '订单详情', roles: ['ROLE_MERCHANT', 'ROLE_ADMIN'] },
      },
      {
        path: '',
        redirect: '/merchant/dashboard',
      },
    ],
  },
  {
    path: '/admin',
    component: AdminLayout,
    meta: { requiresAuth: true, roles: ['ROLE_ADMIN'] },
    children: [
      {
        path: '',
        name: 'Admin',
        component: () => import('@/views/HomePage.vue'), // 临时占位
        meta: { title: '平台管理' },
      },
      {
        path: 'merchants',
        name: 'AdminMerchants',
        component: ReviewListPage,
        meta: { title: '商家审核' },
      },
      {
        path: 'merchants/:id',
        name: 'AdminMerchantDetail',
        component: MerchantDetailPage,
        props: true,
        meta: { title: '商家详情' },
      },
      {
        path: 'fruits',
        name: 'AdminFruits',
        component: FruitListPage,
        meta: { title: '商品目录' },
      },
      {
        path: 'reports',
        name: 'AdminReports',
        component: ReportListPage,
        meta: { title: '举报审核' },
      },
    ],
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: NotFoundPage,
    meta: { title: '404' },
  },
]

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes,
  scrollBehavior() {
    return { top: 0 }
  },
})

// 路由守卫
router.beforeEach((to, from, next) => {
  // 设置页面标题
  document.title = `${to.meta.title || '校园水果商城'} - 校园水果商城`

  const token = getAccessToken()

  // 需要认证但未登录 → 跳转登录页（携带 redirect）
  if (to.matched.some((record) => record.meta.requiresAuth)) {
    if (!token) {
      return next({
        name: 'Login',
        query: { redirect: to.fullPath },
      })
    }

    // 角色权限检查
    const requiredRoles = to.meta.roles as string[] | undefined
    if (requiredRoles && requiredRoles.length > 0) {
      const hasRequiredRole = requiredRoles.some((role) => hasRole(role))
      if (!hasRequiredRole) {
        ElMessage.warning('您没有权限访问该页面')
        return next({ name: 'Home' })
      }
    }

    // 商家页面特殊检查：已入驻商家不应该访问入驻页
    if (to.name === 'MerchantApply' && isMerchant()) {
      return next({ name: 'MerchantDashboard' })
    }
  }

  // 已登录用户访问登录/注册等 guest 页面 → 重定向到首页
  if (to.meta.guest && token) {
    return next({ name: 'Home' })
  }

  next()
})

export function setupRouter(app: App) {
  app.use(router)
}

export default router

import router from './router'
import { ElMessage } from 'element-plus'
import NProgress from 'nprogress'
import 'nprogress/nprogress.css'
import { getToken } from '@/utils/auth'
import { isHttp, isPathMatch } from '@/utils/validate'
import { isRelogin } from '@/utils/request'
import useUserStore from '@/store/modules/user'
import useSettingsStore from '@/store/modules/settings'
import usePermissionStore from '@/store/modules/permission'

NProgress.configure({ showSpinner: false })

const whiteList = ['/login', '/register', '/beatExercise']

const isWhiteList = (path) => {
  // 特殊处理 /analysis 路径 - 仅 /analysis/:id/:name 格式
  const segments = path.split('/');
  if (path.startsWith('/analysis/') && segments.length === 4) {
    console.log('匹配到 analysis 路由白名单:', path);
    return true;
  }
  const result = whiteList.some(pattern => isPathMatch(pattern, path));
  if (result) {
    console.log('匹配到普通白名单:', path);
  }
  return result;
}

router.beforeEach((to, from, next) => {
  NProgress.start()
  
  console.log('路由跳转:', to.path, '白名单匹配:', isWhiteList(to.path), '有Token:', !!getToken())
  
  if (getToken()) {
    to.meta.title && useSettingsStore().setTitle(to.meta.title)
    /* has token*/
    if (to.path === '/login') {
      next({ path: '/' })
      NProgress.done()
    } else if (isWhiteList(to.path)) {
      console.log('白名单路由，直接放行:', to.path)
      next()
    } else {
      if (useUserStore().roles.length === 0) {
        isRelogin.show = true
        useUserStore().getInfo().then(() => {
          isRelogin.show = false
          usePermissionStore().generateRoutes().then(accessRoutes => {
            accessRoutes.forEach(route => {
              if (!isHttp(route.path)) {
                router.addRoute(route)
              }
            })
            next({ ...to, replace: true })
          })
        }).catch(err => {
          useUserStore().logOut().then(() => {
            ElMessage.error(err)
            next({ path: '/' })
          })
        })
      } else {
        next()
      }
    }
  } else {
    if (isWhiteList(to.path)) {
      console.log('无Token，白名单路由，直接放行:', to.path)
      next()
    } else {
      next(`/login?redirect=${to.fullPath}`)
      NProgress.done()
    }
  }
})

router.afterEach(() => {
  NProgress.done()
})

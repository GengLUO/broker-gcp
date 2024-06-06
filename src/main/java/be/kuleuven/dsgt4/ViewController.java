package be.kuleuven.dsgt4;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller //@Controller：注解表明这个类是一个Spring MVC控制器，用于处理Web请求
public class ViewController {

    @GetMapping({"/"}) //将根URL路径（/）映射到 spa() 方法。这意味着当用户访问根路径时，会调用这个方法
    public String spa() {
        return "forward:/index.html";
    } //单页应用（SPA）

    @GetMapping("/_ah/warmup") //通常用于应用程序的热身，不执行任何操作
    public void warmup() {
    }
}

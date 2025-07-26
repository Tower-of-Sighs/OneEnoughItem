# OneEnoughItem

本模组开发的初衷是解决物品辞典重复的问题，三种银四种锌五种番茄六种玉米，还各自有着不同的配方。

使用OEI，可以便捷地将重复物品替换为唯一指定代表物品。

### 典型案例

![e7b458ea](https://resource-api.xyeidc.com/client/pics/e7b458ea)

**整合包里有17种番茄**

![304e2a74](https://resource-api.xyeidc.com/client/pics/304e2a74)

**整合包中里六种某矿石**

而且，他们还有着各自独立的配方！

如今，那样的日子一去不复返了，有了OEI，你可以在物品刚创建时就将其替换为唯一指定代表物品！

### 使用方式

OEI由数据包驱动，你可以通过下面的写法实现非常简单的物品替换：

```JSON
[
    {
        "matchItems": [
            "minecraft:apple",
            "minecraft:potato",
            "minecraft:carrot"
        ],
        "resultItems": "minecraft:egg"
    },
    {
        "matchItems": [
            "minecraft:stone",
            "minecraft:white_wool",
            "minecraft:oak_log"
        ],
        "resultItems": "minecraft:redstone"
    }
]
```

将需要替换的物品id写进一个"matchItems"列表，再将唯一指定代表物品填在"resultItems"字段，即可在游戏中实现自动替换。

### 使用效果

OEI的物品替换并不是实时检测玩家身上的物品再进行替换，而是发生在非常非常初始的阶段。

有多初始呢？

举个例子，我将整个游戏中的物品都替换成了鸡蛋，那么打开创造模式物品就会看见这一幕：

![0d887667](https://resource-api.xyeidc.com/client/pics/0d887667)

是的，从诞生的时刻起，这些物品就不再是原来的自己了！

由此延伸，物品相应的配方也会发生替换，例如这里将铁锭替换成了鸡蛋：

![f0922a1f](https://resource-api.xyeidc.com/client/pics/f0922a1f)

是的，**对于所有物品配方都是自动生效的**。

并且，在与JEI等配方管理器同时使用时，JEI中展示的配方也会相应地变化：

![cce326fc](https://resource-api.xyeidc.com/client/pics/cce326fc)

可以说，只要是JEI所支持的配方，都可以完全自动替换！

就算不支持，在游戏中注册过的配方，只要不是硬编码形式，也可以正常实现替换。

### 与Almost Unified的区别

Almost Unified的作用是为每个配置的标签提供一个主要资源，并使所有配方都使用此主物品。

而OEI完全脱离了标签系统，以决绝的方式进行了物品替换，在统一同类物品的方向上做得更加果决彻底，你将无法在游戏中见到所有被替换的物品。

### 未来计划

- 暂无，欢迎提供建议！
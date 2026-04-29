package com.kioschool.kioschoolapi.global.schedule.script

import com.kioschool.kioschoolapi.domain.product.repository.ProductRepository
import com.kioschool.kioschoolapi.domain.workspace.repository.WorkspaceImageRepository
import com.kioschool.kioschoolapi.global.aws.S3Service
import com.kioschool.kioschoolapi.global.schedule.Runnable
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class V07__ResizeExistingImages(
    private val productRepository: ProductRepository,
    private val workspaceImageRepository: WorkspaceImageRepository,
    private val s3Service: S3Service,
    @Value("\${cloud.aws.s3.default-path}")
    private val defaultPath: String
) : Runnable {

    private val logger = LoggerFactory.getLogger(V07__ResizeExistingImages::class.java)

    override fun run() {
        logger.info("Starting V07__ResizeExistingImages script...")

        val products = productRepository.findAll().filter { it.imageUrl != null }
        products.forEach { product ->
            try {
                val oldUrl = product.imageUrl!!
                if (oldUrl.endsWith(".webp", ignoreCase = true)) return@forEach

                s3Service.downloadFileStream(oldUrl).use { inputStream ->
                    val path =
                        "$defaultPath/workspace${product.workspace.id}/product/product-${product.id}/${System.currentTimeMillis()}.webp"
                    val newUrl = s3Service.uploadResizedWebpImage(inputStream, path)

                    product.imageUrl = newUrl
                    productRepository.save(product)
                    logger.info("Resized product image: ${product.id} -> $newUrl")
                }
            } catch (e: Exception) {
                logger.error("Failed to resize product image: ${product.id}", e)
            }
            Thread.sleep(50)
        }

        val workspaceImages = workspaceImageRepository.findAll()
        workspaceImages.forEach { image ->
            try {
                val oldUrl = image.url
                if (oldUrl.endsWith(".webp", ignoreCase = true)) return@forEach

                s3Service.downloadFileStream(oldUrl).use { inputStream ->
                    val path =
                        "$defaultPath/workspace${image.workspace.id}/workspace/${System.currentTimeMillis()}.webp"
                    val newUrl = s3Service.uploadResizedWebpImage(inputStream, path)

                    image.url = newUrl
                    workspaceImageRepository.save(image)
                    logger.info("Resized workspace image: ${image.id} -> $newUrl")
                }
            } catch (e: Exception) {
                logger.error("Failed to resize workspace image: ${image.id}", e)
            }
            Thread.sleep(50)
        }

        logger.info("Finished V07__ResizeExistingImages script.")
    }
}

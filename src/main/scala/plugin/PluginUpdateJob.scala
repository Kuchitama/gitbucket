package plugin

import org.eclipse.jgit.api.Git
import org.quartz.JobBuilder._
import org.quartz.SimpleScheduleBuilder._
import org.quartz.TriggerBuilder._
import org.quartz.{Job, JobExecutionContext, Scheduler}
import org.slf4j.LoggerFactory
import service.ProxyService
import util.Directory._

class PluginUpdateJob extends Job {

  private val logger = LoggerFactory.getLogger(classOf[PluginUpdateJob])
  private var failedCount = 0

  /**
   * Clone or pull all plugin repositories
   *
   * TODO Support plugin repository access through the proxy server
   */
  override def execute(context: JobExecutionContext): Unit = {
    try {
      if(failedCount > 3){
        logger.error("Skip plugin information updating because failed count is over limit")
      } else {
        logger.info("Start plugin information updating")
        PluginSystem.repositories.foreach { repository =>
          logger.info(s"Updating ${repository.id}: ${repository.url}...")
          val dir = getPluginCacheDir()
          val repo = new java.io.File(dir, repository.id)
          if(repo.exists){
            // pull if the repository is already cloned
            Git.open(repo).pull().call()
          } else {
            // clone if the repository is not exist
            val git = Git.cloneRepository().setURI(repository.url).setDirectory(repo).call()
            val repositoryConfig = git.getRepository.getConfig

            if (ProxyService.isUseProxy) {
              repositoryConfig.setString("remote", "origin", "proxy", ProxyService.proxyAddress)
              repositoryConfig.save()
            }
          }
        }
        logger.info("End plugin information updating")
      }
    } catch {
      case e: Exception => {
        failedCount = failedCount + 1
        logger.error("Failed to update plugin information", e)
      }
    }
  }
}

object PluginUpdateJob {
  def schedule(scheduler: Scheduler): Unit = {
    val job = newJob(classOf[PluginUpdateJob])
      .withIdentity("pluginUpdateJob")
      .build()

    val trigger = newTrigger()
      .withIdentity("pluginUpdateTrigger")
      .startNow()
      .withSchedule(simpleSchedule().withIntervalInHours(24).repeatForever())
      .build()

    scheduler.scheduleJob(job, trigger)
  }

}
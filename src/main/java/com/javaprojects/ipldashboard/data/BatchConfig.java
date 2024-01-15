package com.javaprojects.ipldashboard.data;

import javax.sql.DataSource;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;

import com.javaprojects.ipldashboard.model.Match;

@Configuration
public class BatchConfig {

    private final String[] FIELD_NAMES = new String[] {
        "ID","City","Date","Season","MatchNumber","Team1","Team2","Venue","TossWinner","TossDecision","SuperOver","WinningTeam","WonBy","Margin","method","Player_of_Match","Team1Players","Team2Players","Umpire1","Umpire2"
    }; 

    @Bean
	public FlatFileItemReader<MatchInput> reader() {
		return new FlatFileItemReaderBuilder<MatchInput>()
			.name("MatchItemReader")
			.resource(new ClassPathResource("ipldata.csv"))
			.delimited()
			.names(FIELD_NAMES)
			.targetType(MatchInput.class)
			.build();
	}

    @Bean
    public MatchDataProcessor processor() {
        return new MatchDataProcessor();
    }
    
    @Bean
	public JdbcBatchItemWriter<Match> writer(DataSource dataSource) {
		return new JdbcBatchItemWriterBuilder<Match>()
			.sql("INSERT INTO match (id, city, date, season, team1, team2, venue, toss_winner, toss_decision, match_winner, won_by, result_margin, player_of_match, umpire1, umpire2)" 
              +  "VALUES (:id, :city, :date, :season, :team1, :team2, :venue, :tossWinner, :tossDecision, :matchWinner, :wonBy, :resultMargin, :playerOfMatch, :umpire1, :umpire2)")
			.dataSource(dataSource)
			.beanMapped()
			.build();
	}

    @Bean
    public Job importUserJob(JobRepository jobRepository, Step step1, JobCompletionNotificationListener listener) {
        return new JobBuilder("importUserJob", jobRepository)
                .listener(listener)
                .start(step1)
                .build();
    }

	@Bean
	public Step step1(JobRepository jobRepository, DataSourceTransactionManager transactionManager,
					  FlatFileItemReader<MatchInput> reader, MatchDataProcessor processor, JdbcBatchItemWriter<Match> writer) {
		return new StepBuilder("step1", jobRepository)
			.<MatchInput, Match> chunk(3, transactionManager)
			.reader(reader)
			.processor(processor)
			.writer(writer)
			.build();
	}

}
